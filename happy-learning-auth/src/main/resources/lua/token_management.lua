---
--- Created by hao.zhou.
--- DateTime: 2025/3/7 16:22
---
-- 令牌管理核心逻辑
-- version: 1.2.1
-- fix: 修复旧令牌清理逻辑
--- 增强版令牌管理脚本（支持授权码）
--- version: 2.0.0
--- 更新内容：
--- 1. 增加授权码存储逻辑
--- 2. 支持多 Token 类型关联
--- 3. 优化批量删除性能


--[[
Redis 令牌管理增强脚本
功能：在保证原子性的前提下实现：
1. 自动清理过期令牌
2. 客户端维度令牌数量控制
3. 多设备会话管理
4. 高性能批量操作
参数：
KEYS[1]: 客户端用户令牌集合键 (CLIENT_USER_TOKENS_KEY)
KEYS[2]: 用户令牌集合键 (USER_TOKEN_MAPPING_KEY)
KEYS[3]: 授权记录键前缀 (OAUTH2_AUTHORIZATION_KEY_PREFIX)
KEYS[4]: 设备令牌映射键 (DEVICE_TOKEN_MAPPING_KEY)
accessToken: 新访问令牌值
ARGV[2]: 设备 ID（为空时不记录设备）
ARGV[3]: 过期时间（秒）
ARGV[4]: 序列化的授权对象 JSON
ARGV[5]: 当前时间戳（秒）
ARGV[6]: 最大允许令牌数
ARGV[7]: authorizationId
ARGV[8]: 授权码]]

--- 增强版令牌管理脚本（多模式兼容）
--- version: 3.0.0
--- 功能：
--- 1. 支持授权码、JWT、客户端模式
--- 2. 动态处理不同 Token 类型
--- 3. 严格空值校验
--- 参数：
--- KEYS[1]: 客户端用户令牌集合键 (CLIENT_USER_TOKENS_KEY)
--- KEYS[2]: 用户令牌集合键 (USER_TOKEN_MAPPING_KEY)
--- KEYS[3]: oauth2:authorization:授权记录键前缀 (OAUTH2_AUTHORIZATION_KEY_PREFIX)
--- KEYS[4]: 设备令牌映射键 (DEVICE_TOKEN_MAPPING_KEY)
--- KEYS[5]: 新增：oauth2:client_refresh_token:控制
--- KEYS[6]: 新增：用户-客户端维度控制
--- accessToken: 访问令牌值（可能为空）
--- ARGV[2]: 设备 ID
--- ARGV[3]: 过期时间（秒）
--- ARGV[4]: 序列化的授权对象 JSON
--- ARGV[5]: 当前时间戳（秒）
--- ARGV[6]: 最大允许令牌数
--- ARGV[7]: 授权 ID
--- ARGV[8]: 授权码
--- ARGV[9]: 刷新令牌
--- ARGV[10]: 授权类型（如 "authorization_code", "client_credentials"）


--- version: 4.0.0
--- ARGV[3]: 修改为access_token过期时间（秒）
--- ARGV[11]: 新增refresh_token过期时间(秒)
--- ARGV[12]: 新增auth_code过期时间(秒)

--- 增强版令牌管理脚本（多模式兼容）
--- version: 4.1.0
--- 更新内容：
--- 1. 刷新令牌时自动清理旧访问令牌
--- 2. 修复授权码模式与刷新令牌的兼容性
--- 3. 增强旧令牌关联数据的清理逻辑
--[[ 辅助函数：安全数字转换 ]]--
local function toNum(str)
    if type(str) == "string" then
        -- 先去除可能存在的双引号
        str = string.gsub(str, "^\"(.*)\"$", "%1")
        local num = tonumber(str)
        if not num then
            error("invalid number: " .. str)
        end
        return num
    end
    return str -- 处理可能透传的数字类型
end

-- 使用正则表达式 ^"(.*)"$ 匹配首尾双引号
local function sanitizeArg(value)
    if type(value) == "string" then
        return string.gsub(value, "^\"(.*)\"$", "%1")
    end
    return value
end

local function isNotEmpty(value)
    -- 检查值内容
    redis.log(redis.LOG_NOTICE, "isNotEmpty value: ", value)
    -- 检查类型
    redis.log(redis.LOG_NOTICE, "isNotEmpty type: ", type(value))
    -- redis.log(redis.LOG_NOTICE, "isNotEmpty result: ", value ~= nil and value ~= '' and value ~= '""')
    return value ~= nil and value ~= '' and value ~= '""'
end

--[[ 主逻辑 ]]--
local function main()
    -- 打印所有ARGV参数
    -- 转换所有数字参数
    local accessToken = sanitizeArg(ARGV[1])
    local deviceId = sanitizeArg(ARGV[2])
    local accessTokenExpiresIn = toNum(ARGV[3])
    local authorization = ARGV[4]
    local currentTime = toNum(ARGV[5])
    local maxTokens = toNum(ARGV[6])
    local authId = sanitizeArg(ARGV[7])
    local authCode = sanitizeArg(ARGV[8]) -- 新增参数：授权码
    local refreshToken = sanitizeArg(ARGV[9]) -- 新增参数：刷新令牌
    local grantType = sanitizeArg(ARGV[10])
    local refreshTokenExpiresIn = toNum(ARGV[11])
    local authCodeExpiresIn = toNum(ARGV[12])

    -- 将 print 替换为 redis.log
    redis.log(redis.LOG_NOTICE, "Debug info:", accessToken, deviceId, accessTokenExpiresIn, authorization, currentTime, maxTokens, authId, authCode, refreshToken, grantType, refreshTokenExpiresIn, authCodeExpiresIn)

    --[[ 阶段0：刷新令牌时清理旧令牌 ]]--
    if grantType == 'refresh_token' then
        -- 通过刷新令牌找到旧授权ID,前提是reuse-refresh-tokens设置为true，如果是false那么security会生成新的token
        -- org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider
        --local oldAuthId = redis.call('GET', 'oauth2:refresh_token:' .. refreshToken)
        local clientRefreshToken = redis.call('GET', KEYS[5])
        local oldAuthId = redis.call('GET', 'oauth2:refresh_token:' .. clientRefreshToken)
        if oldAuthId then
            -- 获取旧访问令牌
            local oldAccessToken = redis.call('GET', 'oauth2:authorization:id:' .. oldAuthId)
            if oldAccessToken then
                -- 清理客户端集合和用户集合中的旧令牌
                redis.call('ZREM', KEYS[1], oldAccessToken)
                if isNotEmpty(KEYS[2]) then
                    redis.call('SREM', KEYS[2], oldAccessToken)
                end
                -- 删除旧令牌的所有关联键
                redis.call('DEL', KEYS[3] .. oldAccessToken)  -- 授权记录
                redis.call('DEL', 'oauth2:authorization:token_to_id:' .. oldAccessToken)
                redis.call('DEL', 'oauth2:access_token:' .. oldAccessToken)
            end
            -- 删除旧授权ID的元数据
            redis.call('DEL', 'oauth2:auth_id:' .. oldAuthId)
            redis.call('DEL', 'oauth2:refresh_token:' .. refreshToken)
            redis.call('DEL', 'oauth2:authorization:id:' .. oldAuthId)
        end
    end

    --[[ 阶段 1：过期令牌清理 ]]--
    -- ZREMRANGEBYSCORE 删除所有 score <= currentTime 的过期令牌
    -- 客户端用户令牌集合键 (CLIENT_USER_TOKENS_KEY)
    local expiredTokens = redis.call('ZRANGEBYSCORE', KEYS[1], 0, currentTime)
    redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, currentTime)
    if #expiredTokens > 0 and isNotEmpty(KEYS[2]) then
        redis.call('SREM', KEYS[2], unpack(expiredTokens))
    end

    --[[ 阶段 2：令牌数量控制（仅限需要令牌限制的模式） ]]--
    redis.log(redis.LOG_NOTICE, "step 1 grantType value: ", grantType)
    if grantType ~= 'authorization_code' and grantType ~= 'client_credentials' then
        -- 授权码模式不限制
        local tokenCount = redis.call('ZCARD', KEYS[1])
        if tokenCount >= maxTokens then
            -- 计算需要移除的令牌数量
            local removeCount = tokenCount - maxTokens + 1

            -- 获取待移除的旧令牌（按 score 升序，即最早到期的）
            local oldTokens = redis.call('ZRANGE', KEYS[1], 0, removeCount - 1)

            --[[ 批量删除关联数据 ]]--
            -- 批量删除旧令牌关联数据
            for _, token in ipairs(oldTokens) do
                local tokenToIdKey = 'oauth2:authorization:token_to_id:' .. token
                local oldAuthorizationId = redis.call('GET', tokenToIdKey)
                --redis.call('DEL', tokenToIdKey)   -- 删除 Token→ID 映射

                if oldAuthorizationId then
                    -- 清理授权码映射
                    --local codeKey = 'oauth2:authorization:id:' .. oldAuthorizationId .. ':code'
                    --local oldCode = redis.call('GET', codeKey)
                    --if oldCode then
                    --redis.call('DEL', 'oauth2:authorization_code:' .. oldCode, codeKey)
                    --end
                    redis.call('DEL', 'oauth2:authorization:id:' .. oldAuthorizationId)
                    redis.call('DEL', tokenToIdKey)
                    -- 清理刷新令牌
                    --local refreshToken = redis.call('GET', 'oauth2:authorization:id:' .. oldAuthorizationId .. ':refresh')
                    --if refreshToken then
                    --redis.call('DEL', 'oauth2:refresh_token:' .. refreshToken)
                    --end

                    -- 删除 ID→Token 映射
                    --redis.call('DEL', 'oauth2:authorization:id:' .. oldAuthorizationId)
                    --redis.call('DEL', tokenToIdKey)
                end
                redis.call('DEL', KEYS[3] .. token)
                redis.call('DEL', 'oauth2:access_token:' .. token)

            end

            if #oldTokens > 0 and isNotEmpty(KEYS[2]) then
                -- 构造完整的授权记录键列表
                --local delKeys = {}
                --for i, token in ipairs(oldTokens) do
                --delKeys[i] = KEYS[3] .. token -- 拼接完整键路径
                --end

                -- 批量删除授权记录（减少网络往返）
                --redis.call('DEL', unpack(delKeys))

                -- 从用户令牌集合移除
                redis.call('SREM', KEYS[2], unpack(oldTokens))
            end


            -- 从有序集合移除旧令牌
            redis.call('ZREMRANGEBYRANK', KEYS[1], 0, removeCount - 1)
        end
    end

    redis.log(redis.LOG_NOTICE, "step 2 grantType value: ", grantType)

    --[[ 阶段 3：存储核心授权记录 ]]--
    -- 根据授权类型选择存储策略
    -- redis lua脚本中收到的值包含双引号，导致判断错误String grantType = authorization.getAuthorizationGrantType().getValue();
    if grantType == 'authorization_code' then
        -- 授权码模式：存储授权码与授权ID的映射
        redis.log(redis.LOG_NOTICE, "step 3 authCode value: ", authCode)
        if isNotEmpty(authCode) then
            local oldAuthId = redis.call('GET', 'oauth2:auth_code:' .. authCode)
            if oldAuthId then
                redis.call('DEL', 'oauth2:auth_id:' .. oldAuthId .. ':code')
                redis.call('DEL', 'oauth2:auth_id:' .. oldAuthId)
                redis.call('DEL', 'oauth2:auth_code:' .. authCode)
            end
            redis.call('SET', 'oauth2:auth_code:' .. authCode, authId, 'EX', authCodeExpiresIn)
            redis.call('SET', 'oauth2:auth_id:' .. authId .. ':code', authCode, 'EX', authCodeExpiresIn)
            --redis.call('SET', 'oauth2:auth_id:' .. authId, authorization, 'EX', authCodeExpiresIn)
            redis.log(redis.LOG_NOTICE, "step 4 authCode value: ", authCode)
        end
    end

    -- 其他模式：存储访问令牌和刷新令牌
    redis.log(redis.LOG_NOTICE, "step 1 other authorization")
    if isNotEmpty(accessToken) then
        -- 双重保障删除可能残留的授权码
        if isNotEmpty(authCode) then
            redis.call('DEL', 'oauth2:auth_code:'..authCode)
            redis.call('DEL', 'oauth2:auth_id:'..authId..':code')
        end

        redis.log(redis.LOG_NOTICE, "step 2 other authorization accessToken:", accessToken)
        local newScore = currentTime + accessTokenExpiresIn  -- 过期时间戳作为 score
        redis.call('ZADD', KEYS[1], newScore, accessToken)
        redis.call('EXPIRE', KEYS[1], accessTokenExpiresIn)  -- 对齐令牌有效期
        -- 仅当用户存在时维护用户集合
        if isNotEmpty(KEYS[2]) then
            -- 维护用户令牌集合
            redis.call('SADD', KEYS[2], accessToken)
            redis.call('EXPIRE', KEYS[2], accessTokenExpiresIn)
        end

        -- 删除关联的授权码（确保一次性使用）
        if isNotEmpty(authCode) then
            redis.call('DEL', 'oauth2:auth_code:' .. authCode)
            redis.call('DEL', 'oauth2:auth_id:' .. authId .. ':code')
        end
        -- 存储完整的授权记录（独立键）
        redis.call('SET', KEYS[3] .. accessToken, authorization, 'EX', accessTokenExpiresIn)
        redis.call('SET', 'oauth2:access_token:' .. accessToken, authId, 'EX', accessTokenExpiresIn)
        -- 存储 ID → Token 映射
        redis.call('SET', 'oauth2:authorization:id:' .. authId, accessToken, 'EX', accessTokenExpiresIn)
        -- 存储 Token → ID 映射
        redis.call('SET', 'oauth2:authorization:token_to_id:' .. accessToken, authId, 'EX', accessTokenExpiresIn)

        --redis.call('SET', 'oauth2:auth_id:' .. authId, authorization, 'EX', accessTokenExpiresIn)
        redis.log(redis.LOG_NOTICE, "step 3 other authorization accessToken:", KEYS[2])
    end

    -- 存储刷新令牌映射
    if isNotEmpty(refreshToken) then
        redis.call('SET', 'oauth2:refresh_token:' .. refreshToken, authId, 'EX', refreshTokenExpiresIn)
        redis.call('SET', KEYS[5], refreshToken, 'EX', refreshTokenExpiresIn)
    end


    --[[ 阶段 4：存储通用数据 ]]--
    -- 授权ID到元数据的映射（所有模式）
    redis.call('SET', 'oauth2:auth_id:' .. authId, authorization, 'EX', refreshTokenExpiresIn)

    if isNotEmpty(deviceId) then
        -- 设备 ID -> 令牌映射（相同设备登录时覆盖旧令牌）
        redis.call('HSET', KEYS[4], deviceId, accessToken or authId)
    end

    return 1 -- 返回成功状态
end

-- 执行主函数
return main()