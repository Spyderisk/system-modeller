package uk.ac.soton.itinnovation.security.modelvalidator.dto;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.Data;

public class DTOExperiments {
	public static void main(String[] args) {
		new DTOExperiments();
	}
	
	public DTOExperiments() {
		//tryClass();
		tryJson();
	}
	
	private void tryClass() {
		//ThreatDB threat = new ThreatDB();
		//threat.setUri("a");
		
		String className = "uk.ac.soton.itinnovation.security.modelvalidator.dto.ThreatDB";
		try {
			Class<?> aClass = Class.forName(className);
			ThreatDB threat = (ThreatDB) aClass.newInstance();
			threat.setUri("a");
			System.out.println(threat.getUri());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private void tryJson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(String.class, new StringDeserializer());
		Gson gson = gsonBuilder.create();
		//gson.toJson("var");
		
		JsonObject obj = new JsonObject();
		JsonArray arr = new JsonArray();
		arr.add("bar");
		obj.addProperty("uri", "foo");
		obj.add("has", arr);
		System.out.println(gson.fromJson(obj.toString(), Test.class));
	}
	
	@Data
	class Test {
		String uri;
		String[] has;
	}
	
	static class StringDeserializer implements JsonDeserializer<String> {

		@Override
		public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			// TODO: Check if this is correct
			if (json.isJsonArray()) {
				json = json.getAsJsonArray().iterator().next();
			}
			String string = json.toString();
			return string.substring(1, string.length()-1);
		}
		
	}

}
