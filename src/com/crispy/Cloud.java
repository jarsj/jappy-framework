package com.crispy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringInputStream;

/**
 * Cloud
 * 
 * A single happy java class to deal with EC2.
 * 
 * @author harsh
 * 
 */
public class Cloud {
	/*
	 * Common Stuff.
	 */
	private static final Log LOG = Log.get("cloud");

	public static boolean localMode = false;
	public static boolean connected;
	private static AWSCredentials credentials;

	public static void init(String accessKey, String secretKey) {
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		connected = false;
		// TODO : Probably don't want to do this.
		reloadBuckets();
	}

	private static String defaultSecurityGroup = null;
	private static String defaultKeyPair = null;

	private static ConcurrentHashMap<String, Boolean> mBuckets;
	private Set<String> keys;
	private boolean neverExpire;

	public static void securityGroup(String sg) {
		defaultSecurityGroup = sg;
	}

	public static void keyPair(String kp) {
		defaultKeyPair = kp;
	}

	public static AWSCredentials getCredentials() {
		return credentials;
	}

	private static void reloadBuckets() {
		mBuckets = new ConcurrentHashMap<String, Boolean>();
		try {
			AmazonS3Client client = new AmazonS3Client(credentials);
			for (Bucket b : client.listBuckets()) {
				mBuckets.put(b.getName(), true);
			}
			connected = true;
		} catch (AmazonClientException t) {
			LOG.error("Unable to connect with amazon");
		}
		LOG.info("Initialize S3 with " + mBuckets.size() + " buckets");
	}

	private AmazonS3 s3;
	private String bucket;
	private AccessControlList acl;

	private InstanceType instanceType;

	private Cloud() {
	}

	public static Cloud s3(String bucket) {
		Cloud c = new Cloud();
		c.s3 = new AmazonS3Client(credentials);
		c.bucket = bucket;
		return c;
	}

	/*
	 * EC2 Stuff
	 */

	private AmazonEC2 ec2;
	private String ami;
	private String userData;
	private String securityGroup;
	private String keyPair;
	private String instanceId;

	public static Cloud ec2() {
		Cloud c = new Cloud();
		c.ec2 = new AmazonEC2Client(credentials);
		c.securityGroup = defaultSecurityGroup;
		c.keyPair = defaultKeyPair;
		return c;
	}

	public Cloud create() {
		if (!mBuckets.containsKey(bucket)) {
			s3.createBucket(bucket);
			reloadBuckets();
		}
		return this;
	}

	public Cloud create(String region) {
		if (!mBuckets.containsKey(bucket)) {
			s3.createBucket(bucket, region);
			reloadBuckets();
		}
		return this;
	}

	public Cloud allowRead() {
		if (acl == null) {
			acl = new AccessControlList();
		}
		acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
		return this;
	}

	public Cloud neverExpire() {
		neverExpire = true;
		return this;
	}

	public Cloud ami(String ami) {
		this.ami = ami;
		return this;
	}

	public Cloud userData(String ud) {
		this.userData = ud;
		return this;
	}

	public Cloud type(InstanceType it) {
		this.instanceType = it;
		return this;
	}

	public Cloud instance(String id) {
		this.instanceId = id;
		return this;
	}

	public void terminate() {
		TerminateInstancesRequest tir = new TerminateInstancesRequest();
		tir.withInstanceIds(instanceId);
		ec2.terminateInstances(tir);
	}

	public String launch() {
		RunInstancesRequest rir = new RunInstancesRequest();
		rir.withImageId(ami).withSecurityGroups(securityGroup).withKeyName(keyPair).withInstanceType(instanceType).withMinCount(1).withMaxCount(1)
				.withUserData(new String(Base64.encodeBase64(userData.getBytes())));
		RunInstancesResult resp = ec2.runInstances(rir);
		return resp.getReservation().getInstances().get(0).getInstanceId();
	}
	
	public String[] launch(int min, int max) {
		RunInstancesRequest rir = new RunInstancesRequest();
		rir.withImageId(ami).withSecurityGroups(securityGroup).withKeyName(keyPair).withInstanceType(instanceType).withMinCount(min).withMaxCount(max);
		if (userData != null) {
				rir.withUserData(new String(Base64.encodeBase64(userData.getBytes())));
		}
		RunInstancesResult resp = ec2.runInstances(rir);
		String[] ret = new String[resp.getReservation().getInstances().size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = resp.getReservation().getInstances().get(i).getInstanceId();
		}
		return ret;
	}

	public Cloud cacheKeys() {
		keys = new TreeSet<String>();
		ObjectListing listing = s3.listObjects(new ListObjectsRequest().withBucketName(bucket).withMaxKeys(Integer.MAX_VALUE));
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			keys.add(summary.getKey());
		}
		LOG.info("Loaded " + keys.size() + " keys");
		return this;
	}

	public boolean exists(String key) {
		return keys.contains(key);
	}

	public Set<String> keys() {
		if (keys == null) {
			cacheKeys();
		}
		return keys;
	}

	public String download(String key) throws IOException {
		GetObjectRequest request = new GetObjectRequest(bucket, key);
		S3Object o = s3.getObject(request);
		if (o == null) return null;
		return IOUtils.toString(o.getObjectContent());
	}
	
	public void remove(String key) {
		DeleteObjectRequest dor = new DeleteObjectRequest(bucket, key);
		s3.deleteObject(dor);
	}
	
	public Cloud upload(String key, String data) throws UnsupportedEncodingException {
		PutObjectRequest request = new PutObjectRequest(bucket, key, new StringInputStream(data), new ObjectMetadata());
		s3.putObject(request);
		return this;
	}
	
	public Cloud upload(String key, File value) throws FileNotFoundException {
		ObjectMetadata metadata = new ObjectMetadata();
		if (value.getName().endsWith("png")) {
			metadata.setContentType("image/png");
		} else if (value.getName().endsWith("jpg")) {
			metadata.setContentType("image/jpg");
		}
		if (neverExpire) {
			metadata.setCacheControl("max-age=8640000");
		}

		PutObjectRequest request = new PutObjectRequest(bucket, key, value);
		request.setMetadata(metadata);
		if (acl != null) {
			request = request.withAccessControlList(acl);
		}
		s3.putObject(request);
		return this;
	}

	public Cloud upload(String key, URI url) throws ClientProtocolException, IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			ObjectMetadata metadata = new ObjectMetadata();
			if (url.getPath().endsWith("png")) {
				metadata.setContentType("image/png");
			} else if (url.getPath().endsWith("jpg")) {
				metadata.setContentType("image/jpg");
			}
			if (neverExpire) {
				metadata.setCacheControl("max-age=8640000");
			}
			PutObjectRequest request = new PutObjectRequest(bucket, key, entity.getContent(), metadata);
			if (acl != null) {
				request = request.withAccessControlList(acl);
			}
			s3.putObject(request);
			EntityUtils.consume(entity);
		} else {
			EntityUtils.consume(response.getEntity());
		}
		return this;
	}

	public static void cacheS3(int N) throws IOException {
		int done = 0;
		for (Metadata m : DB.getTables()) {
			for (Column c : m.columns) {
				if (c.comment.startsWith("s3:")) {
					for (Row r : Table.get(m.name).columns(c.name).rows()) {
						URL u = r.columnAsUrl(c.name);
						if (u == null)
							continue;
						if (u.toString().endsWith(".psd"))
							continue;
						if (!Cache.getInstance().existsInCache(u)) {
							Cache.getInstance().fetchUrl(u);
							done++;
							if (done >= N)
								return;
						}
					}
				}
			}
		}
	}

	public static String userData() {
		try {
			return Net.get("http://169.254.169.254/latest/user-data", 5);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return null;
		}
	}
	
	public static String instanceId() {
		try {
			return Net.get("http://169.254.169.254/latest/meta-data/instance-id", 5).trim();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return "local";
		}
	}

	public boolean isTerminated(String instanceId) {
		DescribeInstancesRequest dir = new DescribeInstancesRequest();
		dir.withInstanceIds(instanceId);
		DescribeInstancesResult res = ec2.describeInstances(dir);
		InstanceState state = res.getReservations().get(0).getInstances().get(0).getState();
		return state.getName().equals("terminated");
	}
}
