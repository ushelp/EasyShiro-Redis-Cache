package cn.easyproject.shirorediscache;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisSessionDAO extends AbstractSessionDAO {

	private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);
	/**
	 * shiro-redis的session对象前缀
	 */
	private RedisManager redisManager;
	
	private RedisTemplate<String, Object> redisTemplate;
	
	// 0 - never expire
	private int expire = 0;
	
	/**
	 * The Redis key prefix for the sessions 
	 */
	private String keyPrefix = "shiro_redis_session:";
	
	@Override
	public void update(Session session) throws UnknownSessionException {
		this.saveSession(session);
	}
	
	/**
	 * save session
	 * @param session
	 * @throws UnknownSessionException
	 */
	private void saveSession(Session session) throws UnknownSessionException{
		if(session == null || session.getId() == null){
			logger.error("session or session id is null");
			return;
		}
		
		byte[] key = getByteKey(session.getId());
		byte[] value = SerializeUtils.serialize(session);
		
		
		session.setTimeout(this.getExpire()*1000);		
		
		if(redisTemplate!=null) {
			redisTemplate.opsForValue().set(getKey(session.getId()), value, this.getExpire(), TimeUnit.SECONDS);
		}else {
			this.redisManager.set(key, value, this.getExpire());
		}
		
	}

	public int getExpire() {
		return expire;
	}

	public void setExpire(int expire) {
		this.expire = expire;
	}

	@Override
	public void delete(Session session) {
		if(session == null || session.getId() == null){
			logger.error("session or session id is null");
			return;
		}
		
		
		if(redisTemplate!=null) {
			redisTemplate.delete(this.getKey(session.getId()));
		}else {
			redisManager.del(this.getByteKey(session.getId()));
		}

	}

	@Override
	public Collection<Session> getActiveSessions() {
		Set<Session> sessions = new HashSet<Session>();
		
		if(redisTemplate!=null) {
			Set<String> keys = redisTemplate.keys(this.keyPrefix + "*");
			if(keys != null && keys.size()>0){
				for(String key:keys){
					Session s = (Session)SerializeUtils.deserialize((byte[])redisTemplate.opsForValue().get(key));
					sessions.add(s);
				}
			}
			
		}else {
			Set<byte[]> keys = redisManager.keys(this.keyPrefix + "*");
			if(keys != null && keys.size()>0){
				for(byte[] key:keys){
					Session s = (Session)SerializeUtils.deserialize(redisManager.get(key));
					sessions.add(s);
				}
			}
		}
		
		
		return sessions;
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable sessionId = this.generateSessionId(session);  
        this.assignSessionId(session, sessionId);
        this.saveSession(session);
		return sessionId;
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		if(sessionId == null){
			logger.error("session id is null");
			return null;
		}
		
		Session s=null;
		if(redisTemplate!=null) {
			s = (Session)SerializeUtils.deserialize((byte[])redisTemplate.opsForValue().get(this.getKey(sessionId)));
		}else {
			s = (Session)SerializeUtils.deserialize(redisManager.get(this.getByteKey(sessionId)));
		}
		
		return s;
	}
	
	/**
	 * 获得byte[]型的key
	 * @param key
	 * @return
	 */
	private byte[] getByteKey(Serializable sessionId){
		String preKey = this.keyPrefix + sessionId;
		return preKey.getBytes();
	}
	/**
	 * 获得byte[]型的key
	 * @param key
	 * @return
	 */
	private String getKey(Serializable sessionId){
		String preKey = this.keyPrefix + sessionId;
		return preKey;
	}

	public RedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisManager redisManager) {
		this.redisManager = redisManager;
		
		/**
		 * 初始化redisManager
		 */
		this.redisManager.init();
		this.redisManager.setExpire(this.getExpire());
	}
	

	public RedisTemplate getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * Returns the Redis session keys
	 * prefix.
	 * @return The prefix
	 */
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * Sets the Redis sessions key 
	 * prefix.
	 * @param keyPrefix The prefix
	 */
	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}
	
	
}
