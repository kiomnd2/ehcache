import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.clustered.client.config.builders.ClusteredResourcePoolBuilder;
import org.ehcache.clustered.client.config.builders.ClusteredStoreConfigurationBuilder;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.clustered.common.Consistency;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.net.URI;

public class EhcacheTest {

    private final static String TERRACOTTA_URI = "terracotta://localhost/my-application";

    public static void main(String[] args) {

        CacheManagerBuilder<PersistentCacheManager> clusteredCacheManagerBuilder =
                CacheManagerBuilder.newCacheManagerBuilder()
                        .with(ClusteringServiceConfigurationBuilder.cluster(URI.create("terracotta://localhost/my-application"))
                                .autoCreate(server -> server
                                        .defaultServerResource("primary-server-resource")
                                        .resourcePool("resource-pool-a", 8, MemoryUnit.MB, "secondary-server-resource")
                                        .resourcePool("resource-pool-b", 10, MemoryUnit.MB)));


        PersistentCacheManager cacheManager = clusteredCacheManagerBuilder.build(true);


        // - clustered storage tier
        CacheConfiguration<Long, String> config = CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()

                        // 1. 일반 cache를 위한 heap tier를 구성한다.
                        .heap(2, EntryUnit.ENTRIES)
                        // 2. ClusteredResourcePoolBuilder를 사용하여 server off-heap resource에 size만큼의 cluster tier를 구성한다.
                        .with(ClusteredResourcePoolBuilder.clusteredDedicated("primary-server-resource", 8, MemoryUnit.MB)))
                .withService(ClusteredStoreConfigurationBuilder.withConsistency(Consistency.STRONG))
                .build();

        Cache<Long, String> cache = cacheManager.createCache("clustered-cache-tiered", config);
        cache.put(42L, "1111111");
        cache.put(43L, "2222222");
        cache.put(44L, "3333333");

        System.out.println(cache.get(42L));
        System.out.println(cache.get(43L));
        System.out.println(cache.get(44L));

        cacheManager.close();


    }
}
