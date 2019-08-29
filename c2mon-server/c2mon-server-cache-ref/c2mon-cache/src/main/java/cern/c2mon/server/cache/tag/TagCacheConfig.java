package cern.c2mon.server.cache.tag;

import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.cache.api.factory.AbstractCacheFactory;
import cern.c2mon.server.cache.CacheName;
import cern.c2mon.server.cache.config.AbstractSimpleCacheConfig;
import cern.c2mon.server.cache.loader.CacheLoaderDAO;
import cern.c2mon.server.common.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Szymon Halastra
 * @author Alexandros Papageorgiou Koufidis
 */
@Configuration
public class TagCacheConfig extends AbstractSimpleCacheConfig<Tag> {

  @Autowired
  public TagCacheConfig(AbstractCacheFactory cachingFactory, CacheLoaderDAO<Tag> cacheLoaderDAORef) {
    super(cachingFactory, CacheName.TAG, Tag.class, cacheLoaderDAORef);
  }

  @Override
  public C2monCache<Tag> createCache() {
    return super.createCache();
  }

}
