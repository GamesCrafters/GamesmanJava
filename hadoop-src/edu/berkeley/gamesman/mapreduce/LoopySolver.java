package edu.berkeley.gamesman.mapreduce;

import edu.berkeley.gamesman.core.GamesmanConf;
import edu.berkeley.gamesman.core.State;

import java.io.IOException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class LoopySolver {
	public static class NodeMapper extends
			Mapper<Object, Text, LongWritable, Message> {
		private LongWritable tmp = new LongWritable();
		private LoopyGame game;

		protected void setup(Context context) {
			Configuration conf = context.getConfiguration();
			try {
				GamesmanConf g = new GamesmanConf(conf);
				game = new LoopyGameAdapter<State>(g.getCheckedGame());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * @param key
		 *            line number of file
		 * @param value
		 *            string representation of Node
		 */
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			// it's text representation for now
			Node n = Node.fromString(value.toString());
			// expand the node
			if (!n.expanded) {
				n.expanded = true;
				for (long child : game.getSuccessors(n.hash)) {
					n.rchildren += 1;
					tmp.set(child);
					context.write(tmp, Message.Expand(n.hash));
				}
				// otherwise propagate the solution upwards
			} else if (n.solved && n.parents.length > 0) {
				// send to any (potentially new) parents that haven't gotten the
				// message
				// not sure how to do this if an explicit list isn't kept
				for (long parent : n.parents) {
					tmp.set(parent);
					context.write(tmp,
							Message.Solve(n.hash, n.value, n.remoteness));
				}
				// delete these from our parent list, since they got the message
				n.parents = new long[0];
			}
			tmp.set(n.hash);
			// emit the node itself. this would be unnecessary if we can access
			// the graph during the reduce phase, and can save the changes we
			// made during map using a message or something else
			context.write(tmp, Message.Identity(n));
		}
	}

	public static class MessageReducer extends
			Reducer<LongWritable, Message, LongWritable, Node> {
		private LongWritable result = new LongWritable();
		private LoopyGame game;

		protected void setup(Context context) {
			Configuration conf = context.getConfiguration();
			try {
				GamesmanConf g = new GamesmanConf(conf);
				game = new LoopyGameAdapter<State>(g.getCheckedGame());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * @param key
		 *            hash of node
		 * @param messages
		 *            messages, including the one packing the node
		 */
		public void reduce(LongWritable key, Iterable<Message> messages,
				Context context) throws IOException, InterruptedException {
			Node n = null;
			List<Message> buf = new ArrayList<Message>();
			// we must first identify the target node
			// this won't be necessary if we can access the database here
			for (Message m : messages) {
				if (m.type == Message.IDENTITY)
					n = m.node;
				else
					buf.add(Message.clone(m)); // XXX can only iterate once
			}
			for (Message m : buf) {
				switch (m.type) {
				// either creating a new node or merging multiple parent edges
				case Message.EXPAND:
					if (n != null) {
						n.addParent(m.hash);
					} else {
						n = new Node();
						n.hash = key.get();
						if (game.isPrimitive(n.hash)) {
							n.value = game.evalPrimitive(n.hash);
							n.remoteness = 0;
							n.solved = true;
							n.expanded = true;
						}
						n.addParent(m.hash);
					}
					break;
				// handle solve case - it's impossible for n to be null
				case Message.SOLVE:
					assert n != null;
					if (!n.solved)
						n.update(m); // updates remoteness, value, solved,
										// rchildren
					break;
				}
			}
			// emit the altered node as output
			context.write(key, n);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: loopy-solve <in> <out>");
			System.exit(2);
		}
		Properties props = edu.berkeley.gamesman.core.Configuration
				.readProperties("jobs/TTT.job");
		for (String name : props.stringPropertyNames()) {
			conf.set(name, props.getProperty(name));
		}
		Job job = new Job(conf, "loopy solver");
		System.out.println("-----------");
		System.out.println(job.getConfiguration().get("gamesman.game"));
		System.out.println("-----------");
		job.setJarByClass(LoopySolver.class);
		job.setMapperClass(NodeMapper.class);
		job.setReducerClass(MessageReducer.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Message.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

// vim: ts=2 sw=2
