# EasyShiro-Redis-Cache


基于 Redis 的 Shrio 缓存，支持 Jedis 直连和  Spring-Data-Redis RedisTemplate。

An implement of redis cache can be used by shiro, Support Jedis direct connect and Spring-Data-Redis RedisTemplate.

Thanks [shiro-redis](https://github.com/alexxiyang/shiro-redis).

## Featuter

- **中文**

	1. 支持基于 Jdeis 直接配置 Redis 连接（ `INI`, `XML`）
	
	2. 支持与 Spring 集成时，使用  Spring-Data-Redis 的 `RedisTemplate` （`XML`）
	
	3. 支持与 
[EasyShiro](https://github.com/ushelp/EasyShiro 'EasyShiro') 集成


- **English**

	1. Support direct configuration of Redis connection (`INI`,` XML`)
	
	2. Support for integration with Spring-Data-Redis RedisTemplate (`XML`)
	
	3. Support for integration 
[EasyShiro](https://github.com/ushelp/EasyShiro 'EasyShiro')


## Maven

```XML
<dependency>
	<groupId>cn.easyproject</groupId>
	<artifactId>easyshiro-redis-cache</artifactId>
	<version>2.6.0-RELEASE</version>
</dependency>
```

## Configure


### RedisManager(Jedis)

- shiro.ini

	```properties
	#redisManager
	redisManager = cn.easyproject.shirorediscache.RedisManager
	#optional if you don't specify host the default value is 127.0.0.1
	redisManager.host = 127.0.0.1
	#optional , default value: 6379
	redisManager.port = 6379
	#optional, timeout for jedis try to connect to redis server(In milliseconds), not equals to expire time! 
	redisManager.timeout = 0
	#optional, password for redis server
	redisManager.password = 
	
	#============redisSessionDAO=============
	redisSessionDAO = cn.easyproject.shirorediscache.RedisSessionDAO
	redisSessionDAO.redisManager = $redisManager
	#optional, default value:0, never expire. The expire time is in second
	redisSessionDAO.expire = 1800
	
	sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
	
	sessionManager.sessionDAO = $redisSessionDAO
	securityManager.sessionManager = $sessionManager
	
	
	#============redisCacheManager===========
	cacheManager = cn.easyproject.shirorediscache.RedisCacheManager
	cacheManager.redisManager = $redisManager
	#custom your redis key prefix, if you doesn't define this parameter shiro-redis will use 'shiro_redis_session:' as default prefix
	cacheManager.keyPrefix = users:security:authz:
	securityManager.cacheManager = $cacheManager
	```

- shiro.xml

	```xml
	<!-- shiro filter -->
	<bean id="ShiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="securityManager" ref="securityManager"/>
		
		<!--
		<property name="loginUrl" value="/login.jsp"/>
		<property name="successUrl" value="/home.jsp"/>  
		<property name="unauthorizedUrl" value="/unauthorized.jsp"/>
		-->
		<!-- The 'filters' property is not necessary since any declared javax.servlet.Filter bean  -->
		<!-- defined will be automatically acquired and available via its beanName in chain        -->
		<!-- definitions, but you can perform instance overrides or name aliases here if you like: -->
		<!-- <property name="filters">
			<util:map>
				<entry key="anAlias" value-ref="someFilter"/>
			</util:map>
		</property> -->
		<property name="filterChainDefinitions">
			<value>
				/login.jsp = anon
				/user/** = anon
				/register/** = anon
				/unauthorized.jsp = anon
				/css/** = anon
				/js/** = anon
				
				/** = authc
			</value>
		</property>
	</bean>
	
	<!-- shiro securityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
	
		<!-- Single realm app.  If you have multiple realms, use the 'realms' property instead. -->
		
		<!-- sessionManager -->
		<property name="sessionManager" ref="sessionManager" />
		
		<!-- cacheManager -->
		<property name="cacheManager" ref="cacheManager" />
		
		<!-- By default the servlet container sessions will be used.  Uncomment this line
			 to use shiro's native sessions (see the JavaDoc for more): -->
		<!-- <property name="sessionMode" value="native"/> -->
	</bean>
	<bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>	
	
	<!-- shiro redisManager -->
	<bean id="redisManager" class="cn.easyproject.shirorediscache.RedisManager">
		<property name="host" value="127.0.0.1"/>
		<property name="port" value="6379"/>
		<!-- optional properties:
		<property name="timeout" value="10000"/>
		<property name="password" value="123456"/>
		-->
	</bean>
	
	<!-- redisSessionDAO -->
	<bean id="redisSessionDAO" class="cn.easyproject.shirorediscache.RedisSessionDAO">
		<property name="redisManager" ref="redisManager" />
		<!-- optional, default value:0, never expire. The expire time is in second -->
		<property name="expire" value="1800"/>
	</bean>
	
	<!-- sessionManager -->
	<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
		<property name="sessionDAO" ref="redisSessionDAO" />
	</bean>
	
	<!-- cacheManager -->
	<bean id="cacheManager" class="cn.easyproject.shirorediscache.RedisCacheManager">
		<property name="redisManager" ref="redisManager" />
	</bean>
	```


### Spring-Data-Redis RedisTemplate

- shiro.xml

	RedisTemplate key is must `StringRedisSerializer`.
	
	```XML
	<!-- RedisTemplate Start -->
	<!-- JedisPool -->
	<bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
		<property name="maxTotal" value="${redis.pool.maxTotal}"></property>
		<property name="maxIdle" value="${redis.pool.maxIdle}"></property>
		<property name="maxWaitMillis" value="${redis.pool.maxWaitMillis}"></property>
		<property name="testOnBorrow" value="${redis.pool.testOnBorrow}"></property>
		<property name="testOnReturn" value="${redis.pool.testOnReturn}"></property>
	</bean>
	
	<bean id="jedisConnFactory"
		class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory"
		p:use-pool="true">
		<property name="hostName" value="${redis.host}"></property>
		<property name="port" value="${redis.port}"></property>
		<property name="database" value="${redis.database}"></property>
		<!-- <property name="password" value="pwd456"></property> -->
		<property name="poolConfig" ref="jedisPoolConfig"></property>
	</bean>
	
	<!-- Redis template definition -->
	<bean id="redisTemplate" class="org.springframework.data.redis.core.RedisTemplate"
		p:connection-factory-ref="jedisConnFactory">
		
		<!-- !IMPORTANT: key is must 'StringRedisSerializer' -->
		<property name="keySerializer">
			<bean class="org.springframework.data.redis.serializer.StringRedisSerializer"></bean>
		</property>
		<property name="hashKeySerializer">
			<bean class="org.springframework.data.redis.serializer.StringRedisSerializer"></bean>
		</property>
		<property name="valueSerializer">
			<bean class="org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"></bean>
		</property>
		<property name="hashValueSerializer">
			<bean class="org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"></bean>
		</property>
	</bean>
	<!-- RedisTemplate End-->
	
		<!-- shiro filter -->
	<bean id="ShiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
		<property name="securityManager" ref="securityManager"/>
		
		<!--
		<property name="loginUrl" value="/login.jsp"/>
		<property name="successUrl" value="/home.jsp"/>  
		<property name="unauthorizedUrl" value="/unauthorized.jsp"/>
		-->
		<!-- The 'filters' property is not necessary since any declared javax.servlet.Filter bean  -->
		<!-- defined will be automatically acquired and available via its beanName in chain        -->
		<!-- definitions, but you can perform instance overrides or name aliases here if you like: -->
		<!-- <property name="filters">
			<util:map>
				<entry key="anAlias" value-ref="someFilter"/>
			</util:map>
		</property> -->
		<property name="filterChainDefinitions">
			<value>
				/login.jsp = anon
				/user/** = anon
				/register/** = anon
				/unauthorized.jsp = anon
				/css/** = anon
				/js/** = anon
				
				/** = authc
			</value>
		</property>
	</bean>
	
	<!-- shiro securityManager -->
	<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
	
		<!-- Single realm app.  If you have multiple realms, use the 'realms' property instead. -->
		
		<!-- sessionManager -->
		<property name="sessionManager" ref="sessionManager" />
		
		<!-- cacheManager -->
		<property name="cacheManager" ref="cacheManager" />
		
		<!-- By default the servlet container sessions will be used.  Uncomment this line
			 to use shiro's native sessions (see the JavaDoc for more): -->
		<!-- <property name="sessionMode" value="native"/> -->
	</bean>
	<bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>	
	
	<!-- redisSessionDAO -->
	<bean id="redisSessionDAO" class="cn.easyproject.shirorediscache.RedisSessionDAO">
		<property name="redisTemplate" ref="redisTemplate" />
		<!-- optional, default value:0, never expire. The expire time is in second -->
		<property name="expire" value="1800"/>
	</bean>
	
	<!-- sessionManager -->
	<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
		<property name="sessionDAO" ref="redisSessionDAO" />
	</bean>
	
	<!-- cacheManager -->
	<bean id="cacheManager" class="cn.easyproject.shirorediscache.RedisCacheManager">
		<property name="redisTemplate" ref="redisTemplate" />
	</bean>
	```



> NOTE
> EasyShiro-Redis-Cache don't support SimpleAuthenticationInfo created by this constructor `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName)`.
> Please use `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, String realmName)` instead.


## End

Email：<inthinkcolor@gmail.com>

[http://www.easyproject.cn](http://www.easyproject.cn "EasyProject Home")


**支付宝钱包扫一扫捐助：**

我们相信，每个人的点滴贡献，都将是推动产生更多、更好免费开源产品的一大步。

**感谢慷慨捐助，以支持服务器运行和鼓励更多社区成员。**

<img alt="支付宝钱包扫一扫捐助" src="http://www.easyproject.cn/images/s.png"  title="支付宝钱包扫一扫捐助"  height="256" width="256"></img>



We believe that the contribution of each bit by bit, will be driven to produce more and better free and open source products a big step.

**Thank you donation to support the server running and encourage more community members.**

[![PayPal](http://www.easyproject.cn/images/paypaldonation5.jpg)](https://www.paypal.me/easyproject/10 "Make payments with PayPal - it's fast, free and secure!")


