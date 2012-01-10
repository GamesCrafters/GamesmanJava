package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class DividedSequenceFileOutputFormat<KEY extends WritableSettableComparable<KEY>, OUTVALUE extends Writable>
		extends FileOutputFormat<KEY, OUTVALUE> {
	private final HashMap<Integer, RecordWriter<KEY, OUTVALUE>> writers = new HashMap<Integer, RecordWriter<KEY, OUTVALUE>>();

	@Override
	public RecordWriter<KEY, OUTVALUE> getRecordWriter(
			final TaskAttemptContext context) throws IOException,
			InterruptedException {
		final Configuration conf = context.getConfiguration();
		final Tree<KEY, ?> myTree = ConfParser
				.<KEY, WritableSettable> newTree(conf);
		final CompressionCodec codec;
		final CompressionType compressionType;
		if (getCompressOutput(context)) {
			// find the kind of compression to do
			compressionType = SequenceFileOutputFormat
					.getOutputCompressionType(context);

			// find the right codec
			Class<?> codecClass = getOutputCompressorClass(context,
					DefaultCodec.class);
			codec = (CompressionCodec) ReflectionUtils.newInstance(codecClass,
					conf);
		} else {
			codec = null;
			compressionType = CompressionType.NONE;
		}
		return new RecordWriter<KEY, OUTVALUE>() {

			@Override
			public void write(KEY key, OUTVALUE value) throws IOException,
					InterruptedException {
				int division = myTree.getDivision(key);
				RecordWriter<KEY, OUTVALUE> writer = getWriter(division);
				writer.write(key, value);
			}

			private RecordWriter<KEY, OUTVALUE> getWriter(int division)
					throws IOException {
				RecordWriter<KEY, OUTVALUE> writer = writers.get(division);
				if (writer == null) {
					writer = createWriter(context, conf, codec,
							compressionType, division);
					writers.put(division, writer);
				}
				return writer;
			}

			@Override
			public void close(TaskAttemptContext context) throws IOException,
					InterruptedException {
				for (RecordWriter<KEY, OUTVALUE> writer : writers.values())
					writer.close(context);
			}
		};
	}

	private RecordWriter<KEY, OUTVALUE> createWriter(
			TaskAttemptContext context, Configuration conf,
			CompressionCodec codec, CompressionType compressionType, int tier)
			throws IOException {
		// get the path of the temporary output file
		Path file = getDefaultWorkFile(context,
				String.format(ConfParser.EXTENSION_FORMAT, tier));
		FileSystem fs = file.getFileSystem(conf);
		final SequenceFile.Writer out = SequenceFile.createWriter(fs, conf,
				file, context.getOutputKeyClass(),
				context.getOutputValueClass(), compressionType, codec, context);

		return new RecordWriter<KEY, OUTVALUE>() {

			@Override
			public void write(KEY key, OUTVALUE value) throws IOException {
				out.append(key, value);
			}

			@Override
			public void close(TaskAttemptContext context) throws IOException {
				out.close();
			}
		};
	}
}
