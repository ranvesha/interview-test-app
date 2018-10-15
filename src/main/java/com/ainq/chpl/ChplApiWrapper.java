package com.ainq.chpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChplApiWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChplApiWrapper.class);
	private static final String PROPERTIES_FILE_NAME = "environment.properties";
	private static ChplApiWrapper instance = null;

	public static final String CHPL_API_URL_BEGIN_PROPERTY = "chplApiUrlBegin";
	public static final String STATUS_ENDPOINT = "statusEndpoint";
	public static final String EDUCATION_TYPES_ENDPOINT = "educationTypesEndpoint";
	public static final String PRACTICE_TYPE_NAMES_ENDPOINT = "practiceTypeNamesEndpoint";
	public static final String SEARCH_ENDPOINT = "searchApi";
	public static final String DETAILS_ENDPOINT = "detailsApi";

	private final Map<String, String> endpoints = new HashMap<>();
	private final Properties properties;
	private final String apiKey;

	/**
	 * private constructor to force not to create instance from outside of this class.
	 */
	private ChplApiWrapper() {
		try {
			this.properties = loadProperties();
			this.apiKey = this.properties.getProperty("apiKey");
			this.populateServiceUrls();
		} catch (IOException ex) {
			String errorMsg = String.format("Could not read properties from file {}", PROPERTIES_FILE_NAME);
			LOGGER.error(errorMsg, ex);
			throw new RuntimeException(errorMsg, ex);
		}
	}

	/**
	 * Get the instance of the CHPL API. Only one instance will be created.
	 */
	public static ChplApiWrapper getInstance() {
		if (instance == null) {
			synchronized (ChplApiWrapper.class) {
				// double null check to ensure single instance is created in multi-threaded environment.
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
		String responseString = "";
		String statusEndpoint = endpoints.get(STATUS_ENDPOINT);

		LOGGER.debug("Making HTTP GET call to {}", statusEndpoint);

		try{
			HttpResponse response = Request.Get(statusEndpoint)
					.version(HttpVersion.HTTP_1_1)
					.execute().returnResponse();

			if (response != null) {
				return response.getStatusLine().getReasonPhrase();
			}
		} catch (IOException e){
			LOGGER.error("Failed to make call to {}", statusEndpoint);
			LOGGER.error("Please check that the {} and statusApi properties are configured correctly in {}",
																	CHPL_API_URL_BEGIN_PROPERTY, PROPERTIES_FILE_NAME);
		}
		return responseString;
	}

	/**
	 * Query the CHPL API to get a list of the education levels.
	 * @return A list of certification body names.
	 */
	public List<String> getEducationLevelNames() {
		JsonObject response;
		List<String> result = null;
		String educationTypesEndpoint = endpoints.get(EDUCATION_TYPES_ENDPOINT);

		LOGGER.debug("Making HTTP GET call to {} with API Key {}", educationTypesEndpoint, apiKey);
		try{
			String jsonResponse = sendRequest(educationTypesEndpoint);
			if (jsonResponse != null) {
				response = new Gson().fromJson(jsonResponse, JsonObject.class);
				LOGGER.debug("Response from {} : \n\t", educationTypesEndpoint, response.toString());
				JsonArray dataArray = response.getAsJsonArray("data");
				result = new ArrayList<String>(dataArray.size());
				for (int i = 0; i < dataArray.size(); i++) {
					JsonObject educationObj = dataArray.get(i).getAsJsonObject();
					String educationName = educationObj.get("name").getAsString();
					result.add(educationName);
				}
			}
		} catch (IOException e){
			LOGGER.error("Failed to make call to {}", educationTypesEndpoint);
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
		JsonArray response;
		List<String> result = null;
		String practiceTypeNamesUrl = endpoints.get(PRACTICE_TYPE_NAMES_ENDPOINT);

		LOGGER.debug("Making HTTP GET call to {} with API Key {}", practiceTypeNamesUrl, apiKey);
		try{
			String jsonResponse = sendRequest(practiceTypeNamesUrl);
			if (jsonResponse != null) {
				response = new Gson().fromJson(jsonResponse, JsonArray.class);
				LOGGER.debug("Response from {} : \n\t", practiceTypeNamesUrl, response.toString());
				result = new ArrayList<>(response.size());
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
	public Set<String> getEducationLevelsForSpecificListings() throws IOException, JSONException{
		Set<String> educationTypeNames = new HashSet<>();
		String educationLevelForSpecificListingEndpoint = endpoints.get(SEARCH_ENDPOINT);
		try {
			String searchResult = sendRequest(educationLevelForSpecificListingEndpoint);
			if (searchResult != null) {
				JSONObject search = new JSONObject(searchResult);
				JSONArray searchArr = search.getJSONArray("results");
				for (Integer i = 0; i < searchArr.length(); i++) {
					if (searchArr.optJSONObject(i).has("product")) {
						String productId = searchArr.optJSONObject(i).get("id").toString();
						getEducationTypeDetails(productId, educationTypeNames);
					}
				}
			}
		}catch (IOException e){
			LOGGER.error("Failed to make call to {}", educationLevelForSpecificListingEndpoint);
			LOGGER.error("Please check that the {} and searchApi properties are configured correctly in {}",
					CHPL_API_URL_BEGIN_PROPERTY, PROPERTIES_FILE_NAME);
		}
		return educationTypeNames;
	}

	/**
	 * Get the details of each Listing
	 * @param productId
	 * @param educationTypeNames
	 * @throws IOException
	 * @throws JSONException
	 */
	private void getEducationTypeDetails(String productId, Set<String> educationTypeNames) throws IOException, JSONException{
		String detailsUrl = String.format(endpoints.get(DETAILS_ENDPOINT), productId);
		String result = sendRequest(detailsUrl);
		JSONObject detailsObj = new JSONObject(result);
		if (detailsObj.has("sed")) {
			JSONObject sedObj = detailsObj.getJSONObject("sed");
			if (sedObj.has("testTasks")) {
				JSONArray testTasksArray = sedObj.getJSONArray("testTasks");
				for (Integer j = 0; j < testTasksArray.length(); j++) {
					if (testTasksArray.optJSONObject(j).has("testParticipants")) {
						JSONArray testParticipantsArray = testTasksArray.optJSONObject(j).getJSONArray("testParticipants");
						for (Integer k = 0; k < testParticipantsArray.length(); k++) {
							educationTypeNames.add(testParticipantsArray.optJSONObject(k).get("educationTypeName").toString());
						}
					}
				}
			}
		}
	}

	/**
	 * Method to get the Endpoints
	 * @return endpoint URLs
	 */
	public Map<String, String> getEndpoints() {
		return this.endpoints;
	}

	/**
	 * Makes the HTTP call to the Endpoints
	 * @param serviceUrl
	 * @return HTTP response as String
	 * @throws IOException
	 */
	private String sendRequest(String serviceUrl) throws IOException{
		String jsonResponse = null;
		jsonResponse =  Request.Get(serviceUrl)
				.version(HttpVersion.HTTP_1_1)
				.addHeader("API-Key", apiKey)
				.execute().returnContent().asString();
		return  jsonResponse;
	}

	/**
	 * loading properties into local instance.
	 * @return - Properties instance
	 * @throws IOException - if fails to load the configuration file from classpath.
	 */
	private Properties loadProperties() throws IOException {
		Properties properties = new Properties();
		final InputStream in = ChplApiWrapper.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
		properties.load(in);
		in.close();
		LOGGER.info("Loaded {} properties from file {}", properties.size(), PROPERTIES_FILE_NAME);
		for (Object property : properties.keySet()) {
			String key = (String) property;
			properties.setProperty(key, properties.getProperty(key).trim());
			LOGGER.info("{} = {}", key, properties.getProperty(key));
		}
		return properties;
	}

	/**
	 * method that populates creates endpoints map.
	 */
	private void populateServiceUrls() {
		endpoints.put(STATUS_ENDPOINT, String.format("%s%s",
				this.properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY)
				,  this.properties.getProperty("statusApi")));

		endpoints.put(EDUCATION_TYPES_ENDPOINT, String.format("%s%s",
				this.properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY)
				,  this.properties.getProperty("educationTypesApi")));

		endpoints.put(PRACTICE_TYPE_NAMES_ENDPOINT, String.format("%s%s",
				this.properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY)
				,  this.properties.getProperty("practiceTypeNamesApi")));

		endpoints.put(SEARCH_ENDPOINT, String.format("%s%s",
				this.properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY)
				,  this.properties.getProperty("searchApi")));
		endpoints.put(DETAILS_ENDPOINT, String.format("%s%s",
				this.properties.getProperty(CHPL_API_URL_BEGIN_PROPERTY)
				,  this.properties.getProperty("detailsApi")));
	}
}
