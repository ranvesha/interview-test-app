package com.ainq.chpl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.lang.reflect.Field;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

public class FailingUnitTests {
	private static ChplApiWrapper api;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		api = ChplApiWrapper.getInstance();
	}

	/**
	 * Get the CHPL application status from the API Wrapper.
	 * The value expected to be returned from the API Wrapper is "OK".
	 */
	@Test
	public void testChplStatusIsOk() {
		String apiStatus = api.getChplStatus();
		assertEquals("OK", apiStatus);
	}

	/**
	 * Get the CHPL application status from the API Wrapper.
	 * The value expected to be returned from the API Wrapper is "OK".
	 */
	@Test
	public void testChplStatusForErrorMessage() {
		// prepare
		Map<String, String> endpoints = api.getEndpoints();
		String origValue = endpoints.get(ChplApiWrapper.STATUS_ENDPOINT);
		endpoints.put(ChplApiWrapper.STATUS_ENDPOINT, "https://chpl.ahrqstg.org/rest1/status");

		// execute
		String apiStatus = api.getChplStatus();
		assertEquals("Not Found", apiStatus);

		// reset
		endpoints.put(ChplApiWrapper.STATUS_ENDPOINT, origValue);
	}
	
	/**
	 * Get the list of sorted education levels from the API Wrapper.
	 */
	@Test
	public void testGetSortedEducationLevels() {
		List<String> educationLevels = api.getSortedEducationLevelNames();
		assertEquals(8, educationLevels.size());
		
		//are they sorted?
		for (int i = 0; i < educationLevels.size() - 1; i++) {
			String curr = educationLevels.get(i);
			String next = educationLevels.get(i+1);
	        if (curr.compareToIgnoreCase(next) > 0) {
	            fail("The education levels are not sorted because " + curr + " and " + next + " are out of order.");
	        }
	    }
	}
	
	/**
	 * Get the practice type names from the API wrapper.
	 * Expected Ambulatory, Inpatient
	 */
	@Test
	public void testGetPraticeTypeNames() {
		List<String> practiceTypes = api.getPracticeTypeNames();
		assertEquals(2, practiceTypes.size());
		
		boolean foundAmbulatory = false;
		boolean foundInpatient = false;
		for(String practiceType : practiceTypes) {
			switch(practiceType.toUpperCase()) {
			case "AMBULATORY":
				foundAmbulatory = true;
				break;
			case "INPATIENT":
				foundInpatient = true;
				break;
			default:
				fail("Unexpected practice type found: " + practiceType);
			}
		}
		assertTrue(foundAmbulatory);
		assertTrue(foundInpatient);
	}
	
	@Test
	public void testGetEducationLevelsForSpecificListings() throws IOException, JSONException{
		Set<String> educationLevels = api.getEducationLevelsForSpecificListings();
		assertEquals(4, educationLevels.size());
		
		boolean foundBachelors = false, foundDoctorate = false, foundAssociate = false, foundMasters = false;
		for(String educationLevel : educationLevels) {
			switch(educationLevel) {
			case "Bachelor's degree":
				foundBachelors = true;
				break;
			case "Doctorate degree (e.g., MD, DNP, DMD, PhD)":
				foundDoctorate = true;
				break;
			case "Associate degree":
				foundAssociate = true;
				break;
			case "Master's degree":
				foundMasters = true;
				break;
			default:
				fail("Unexpected education level found: " + educationLevel);
			}
		}
		assertTrue(foundBachelors && foundDoctorate && foundAssociate && foundMasters);
	}
}
