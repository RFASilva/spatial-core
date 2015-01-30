package vast_research.spatial_core.resources;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public final class Workers {

	public static String HAZELCAST_NAME = null;

	public static HazelcastInstance init() {
		HazelcastInstance instance;
		if (HAZELCAST_NAME == null) {
			instance = Hazelcast.newHazelcastInstance();
			HAZELCAST_NAME = instance.getName();
		} else
			instance = Hazelcast.getHazelcastInstanceByName(HAZELCAST_NAME);
		return instance;
	}

	public static void main(final String[] args) {
		Workers wks = new Workers();
	}

	public static <V> Future<V> submit(final Callable<V> callable) {
		HazelcastInstance instance = init();
		return instance.getExecutorService("default").submit(callable);
	}

	public static <V> void submit(final Callable<V> callable,
			final ExecutionCallback<V> callback) {
		HazelcastInstance instance = init();
		instance.getExecutorService("default").submit(callable, callback);
	}

	public Workers() {
		HazelcastInstance instance = init();
	}
}
