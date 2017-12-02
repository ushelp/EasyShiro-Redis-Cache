package cn.easyproject.shirorediscache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis Template Cache implements
 * 
 * @author Ray
 * @author inthinkcolor@gmail.com
 * @author easyproject.cn
 * @since 2.6.0
 */
public class RedisTemplateCache<K, V> implements Cache<K, V> {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
		
	/**
     * The wrapped Jedis instance.
     */
	private RedisTemplate<String, Object> cache;
	
	/**
	 * The Redis key prefix for the sessions 
	 */
	private String keyPrefix = "shiro_redis_session:";
	
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
	
	/**
	 * 通过一个JedisManager实例构造RedisCache
	 */
	public RedisTemplateCache(RedisTemplate<String, Object> cache){
		 if (cache == null) {
	         throw new IllegalArgumentException("Cache argument cannot be null.");
	     }
	     this.cache = cache;
	}
	
	/**
	 * Constructs a cache instance with the specified
	 * Redis manager and using a custom key prefix.
	 * @param cache The cache manager instance
	 * @param prefix The Redis key prefix
	 */
	public RedisTemplateCache(RedisTemplate<String, Object> cache, 
				String prefix){
		 
		this( cache );
		
		// set the prefix
		this.keyPrefix = prefix;
	}
	
	/**
	 * 获得byte[]型的key
	 * @param key
	 * @return
	 */
	private String getKey(K key){
		String preKey = this.keyPrefix + key;
		return preKey;
	}
 	
	@Override
	public V get(K key) throws CacheException {
		logger.debug("根据key从Redis中获取对象 key [" + key + "]");
		try {
			if (key == null) {
	            return null;
	        }else{
	        	byte[] rawValue = (byte[]) cache.opsForValue().get(getKey(key));
	        	@SuppressWarnings("unchecked")
				V value = (V)SerializeUtils.deserialize(rawValue);
	        	return value;
	        }
		} catch (Throwable t) {
			throw new CacheException(t);
		}

	}

	@Override
	public V put(K key, V value) throws CacheException {
		logger.debug("根据key从存储 key [" + key + "]");
		 try {
				System.out.println("==========================================2");
				System.out.println(key);
				System.out.println(new String(getKey(key)));
				System.out.println("==========================================2");
			 
			 	cache.opsForValue().set(getKey(key), SerializeUtils.serialize(value));
	            return value;
	        } catch (Throwable t) {
	            throw new CacheException(t);
	        }
	}

	@Override
	public V remove(K key) throws CacheException {
		logger.debug("从redis中删除 key [" + key + "]");
		try {
            V previous = get(key);
            
            System.out.println("==========================================3");
			System.out.println(key);
			System.out.println(new String(getKey(key)));
			System.out.println("==========================================3");
            
            cache.delete(getKey(key));
            return previous;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public void clear() throws CacheException {
		logger.debug("从redis中删除所有元素");
		try {
            cache.execute(new RedisCallback<String>() {
				@Override
				public String doInRedis(RedisConnection connection) throws DataAccessException {
					 connection.flushDb();
					 return "ok";
				}
			});
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public int size() {
		try {
			Long longSize = cache.execute(new RedisCallback<Long>() {
				@Override
				public Long doInRedis(RedisConnection connection) throws DataAccessException {
					 return connection.dbSize();
				}
			});
			
            return longSize.intValue();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<K> keys() {
		try {
            Set<String> keys = cache.keys(this.keyPrefix + "*");
            if (CollectionUtils.isEmpty(keys)) {
            	return Collections.emptySet();
            }else{
            	Set<K> newKeys = new HashSet<K>();
            	for(String key:keys){
            		newKeys.add((K)key);
            	}
            	return newKeys;
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public Collection<V> values() {
		try {
            Set<String> keys = cache.keys(this.keyPrefix + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                List<V> values = new ArrayList<V>(keys.size());
                for (String key : keys) {
                    @SuppressWarnings("unchecked")
					V value = get((K)key);
                    if (value != null) {
                        values.add(value);
                    }
                }
                return Collections.unmodifiableList(values);
            } else {
                return Collections.emptyList();
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

}
