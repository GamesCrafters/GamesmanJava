package edu.berkeley.gamesman.propogater.factory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.writable.Resetable;

public class FactoryUtil {
	public static <T> Factory<T> makeFactory(
			final Class<? extends T> factClass, final Configuration conf) {
		return new Factory<T>() {
			@Override
			public T create() {
				return ReflectionUtils.newInstance(factClass, conf);
			}

			@Override
			public void reset(T obj) {
				if (obj instanceof Resetable)
					((Resetable) obj).reset();
			}
		};
	}
}
