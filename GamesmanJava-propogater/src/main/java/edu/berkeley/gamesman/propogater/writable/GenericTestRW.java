package edu.berkeley.gamesman.propogater.writable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;
import org.junit.Assert;

public class GenericTestRW {
	public static Writable makeTestCopy(Writable orig,
			Class<? extends Writable> fillClass, Configuration conf)
			throws IOException {
		Writable v = ReflectionUtils.newInstance(fillClass, conf);
		rwCopyTo(orig, v);
		return v;
	}

	public static void rwCopyTo(Writable orig, Writable toFill)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		orig.write(out);
		out.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		DataInputStream in = new DataInputStream(bais);
		toFill.readFields(in);
	}

	public static void testEqualsCreate(Writable orig) throws IOException {
		Assert.assertEquals(orig,
				makeTestCopy(orig, orig.getClass(), getConf(orig)));
	}

	private static Configuration getConf(Writable orig) {
		if (orig instanceof Configurable)
			return ((Configurable) orig).getConf();
		else
			return null;
	}
}
