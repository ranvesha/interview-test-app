package com.ainq.chpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChplApiWrapper {

	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ChplApiWrapper.class);

	private static final String PROPERTIES_FILE_NAME = "environment.properties";
	private static final String CHPL_API_URL_BEGIN_PROPERTY = "chplApiUrlBegin";
	private static ChplApiWrapper instance = null;
	private Properties properties;
	private static String apiKey = null;

	private static String chplStatusUrl = null;
	private static String educationLevelNamesUrl = null;
	private static String practiceTypeNamesUrl = null;

	private ChplApiWrapper() {
		try{
			loadProperties();
			populateServiceUrls();
		}
		catch (IOException ex) {
			LOGGER.error("Could not read properties from file {}", PROPERTIES_FILE_NAME, ex);
			throw new RuntimeException(ex);
		}
	}
	private void loadProperties() throws IOException {
		properties = new Properties();
		final InputStream in = ChplApiWrapper.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
		properties.load(in);
		in.close();
		LOGGER.info("Loaded {} properties from file {}", properties.size(), PROPERTIES_FILE_NAME);
		for (Object property : properties.keySet()) {
			String key = (String) property;
			properties.setProperty(key, properties.getProperty(key).trim());
			LOGGER.info("{} = {}", key, properties.getProperty(key));
		}
	}
	private void populateServiceUrls(){
		apiKey = properties.getProperty("apiKey");
		chplStatusUrl = properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY) + properties.getProperty("statusApi");
		educationLevelNamesUrl = properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY) + properties.getProperty("educationTypesApi");
		practiceTypeNamesUrl = properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY) + properties.getProperty("practiceTypeNamesApi");
	}
	/**
	 * Get the instance of the CHPL API. Only one instance will be created.
	 */
	public static ChplApiWrapper getInstance() {
		if(instance == null) {
			synchronized (ChplApiWrapper.class) {
				if (instance == null) {
					instance = new ChplApiWrapper();
				}
			}
		} 
		return instance;
	}
	
	/**
	 * Get the status of the CHPL API. Most of the time this should return "OK".
	 * If the CHPL API is not available, an HTTP error code may be returned
	 * from the API or simply no response if the server itself is down.
	 * @return
	 */
	public String getChplStatus() {
		LOGGER.debug("Making HTTP GET call to {}", chplStatusUrl);
		
		JsonObject response = null;
		try{
			String jsonResponse = Request.Get(chplStatusUrl)
					.version(HttpVersion.HTTP_1_1)
					.execute().returnContent().asString();
			response = new Gson().fromJson(jsonResponse, JsonObject.class);
		} catch (IOException e){
			LOGGER.error("Failed to make call to {}", chplStatusUrl);
			LOGGER.error("Please check that the {} and statusApi properties are configured correctly in {}",
																	CHPL_API_URL_BEGIN_PROPERTY, PROPERTIES_FILE_NAME);
		}
		LOGGER.debug("Response from {}: \n\t", response.toString());
		return response.getAsJsonPrimitive("status").getAsString();
	}
	
	
	/**
	 * Query the CHPL API to get a list of the education levels.
	 * @return A list of certification body names.
	 */
	public List<String> getEducationLevelNames() {
		LOGGER.debug("Making HTTP GET call to {} with API Key {}", educationLevelNamesUrl, apiKey);
		JsonObject response = null;
		List<String> result = null;
		try{
			String jsonResponse = makeRequest(educationLevelNamesUrl);
			if (jsonResponse != null) {
				response = new Gson().fromJson(jsonResponse, JsonObject.class);
				LOGGER.debug("Response from {} : \n\t", educationLevelNamesUrl, response.toString());
				JsonArray dataArray = response.getAsJsonArray("data");
				result = new ArrayList<String>(dataArray.size());
				for (int i = 0; i < dataArray.size(); i++) {
					JsonObject educationObj = dataArray.get(i).getAsJsonObject();
					String educationName = educationObj.get("name").getAsString();
					result.add(educationName);
				}
			}
		} catch (IOException e){
			LOGGER.error("Failed to make call to {}", educationLevelNamesUrl);
			LOGGER.error("Please check that the {} and educationTypesApi properties are configured correctly in {}",
					CHPL_API_URL_BEGIN_PROPERTY, PROPERTIES_FILE_NAME);
		}
		return result;
	}
	
	/**
	 * This method should return a list of the education level names
	 * sorted alphabetically (A -> Z).
	 * @return A sorted list of education types.
	 */
	public List<String> getSortedEducationLevelNames() {
		List<String> result = getEducationLevelNames();
		Collections.sort(result);
		return result;
	}
	
	/**
	 * This method should call https://chpl.ahrqstg.org/rest/data/practice_types
	 * and parse the "name" field from each element to get a list of
	 * practice type names.
	 * @return A list of practice type names.
	 */
	public List<String> getPracticeTypeNames() {
		LOGGER.debug("Making HTTP GET call to {} with API Key {}", practiceTypeNamesUrl, apiKey);
		JsonArray response;
		List<String> result = null;
		try{
			String jsonResponse = makeRequest(practiceTypeNamesUrl);
			if (jsonResponse != null) {
				response = new Gson().fromJson(jsonResponse, JsonArray.class);
				LOGGER.debug("Response from {} : \n\t", practiceTypeNamesUrl, response.toString());
				result = new ArrayList<String>(response.size());
				for(int i = 0; i < response.size(); i++) {
					JsonObject educationObj = response.get(i).getAsJsonObject();
					String educationName = educationObj.get("name").getAsString();
					result.add(educationName);
				}
				return result;
			}
		} catch (IOException e){
			LOGGER.error("Failed to make call to {}", practiceTypeNamesUrl);
			LOGGER.error("Please check that the {} and practiceTypeNamesApi properties are configured correctly in {}",
					CHPL_API_URL_BEGIN_PROPERTY, PROPERTIES_FILE_NAME);
		}
		return result;
	}
	
	/**
	 * Which education levels did test participants have for listings
	 * with a 2015 certification edition and certified between March 1, 2017 and 
	 * March 31, 2017?
	 * HINT: Get listings matching the description above using our search API.
	 * The summary data sent back from the search API will not have test participants
	 * but the details API will give that information. 
	 * In the details of a given listing, test participants are nested under each 
	 * certificationResult -> testTasks -> testParticipants and not all certificationResults
	 * will have them so you may have to dig a bit to find them.
	 */
	public Set<String> getEducationLevelsForSpecificListings() {
		//TODO: implement this method!
		return null;
	}

	private String makeRequest(String serviceUrl) throws IOException{
		String jsonResponse = null;
		jsonResponse =  Request.Get(serviceUrl)
				.version(HttpVersion.HTTP_1_1)
				.addHeader("API-Key", apiKey)
				.execute().returnContent().asString();
		return  jsonResponse;
	}
}
