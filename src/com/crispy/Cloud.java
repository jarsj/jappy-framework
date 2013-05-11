package com.crispy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class Cloud {
	private static final Log LOG = Log.get("cloud");

	private static AWSCredentials credentials;
	private static ConcurrentHashMap<String, Boolean> mBuckets;
	private Set<String> keys;
	private boolean neverExpire;

	public static void init(String accessKey, String secretKey) {
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		reloadBuckets();
	}
	
	public static AWSCredentials getCredentials() {
		return credentials;
	}

	private static void reloadBuckets() {
		mBuckets = new ConcurrentHashMap<String, Boolean>();
		AmazonS3Client client = new AmazonS3Client(credentials);
		for (Bucket b : client.listBuckets()) {
			mBuckets.put(b.getName(), true);
		}
		LOG.info("Initialize S3 with " + mBuckets.size() + " buckets");
	}
	
	private AmazonS3 s3;
	private String bucket;
	private AccessControlList acl;

	private Cloud() {
	}

	public static Cloud s3(String bucket) {
		Cloud c = new Cloud();
		c.s3 = new AmazonS3Client(credentials);
		c.bucket = bucket;
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

	public Cloud cacheKeys() {
		keys = new TreeSet<String>();
		ObjectListing listing = s3.listObjects(new ListObjectsRequest()
				.withBucketName(bucket).withMaxKeys(Integer.MAX_VALUE));
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

	public Cloud upload(String key, File value) throws FileNotFoundException {
		ObjectMetadata metadata = new ObjectMetadata();
		if (value.getName().endsWith("png")) {
			metadata.setContentType("image/png");
		} else if (value.getName().endsWith("jpg")) {
			metadata.setContentType("image/jpg");
		}
		if (neverExpire) {
			metadata.setCacheControl("max-age=86400");
		}
		
		PutObjectRequest request = new PutObjectRequest(bucket, key,
				new FileInputStream(value), metadata);
		if (acl != null) {
			request = request.withAccessControlList(acl);
		}
		s3.putObject(request);
		return this;
	}

	public Cloud upload(String key, String url) throws ClientProtocolException,
			IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			ObjectMetadata metadata = new ObjectMetadata();
			if (url.endsWith("png")) {
				metadata.setContentType("image/png");
			} else if (url.endsWith("jpg")) {
				metadata.setContentType("image/jpg");
			}
			if (neverExpire) {
				metadata.setCacheControl("max-age=86400");
			}
			PutObjectRequest request = new PutObjectRequest(bucket, key,
					entity.getContent(), metadata);
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
}
