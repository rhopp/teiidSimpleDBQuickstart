package cz.muni.hopp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;

public class ImportData {

	private static Map<Integer, Movie> movies;

	private static String keyId;

	private static String secretKey;

	private static Map<Person, Integer> peopleMap;

	private static AmazonSimpleDBClient client;
	
	private static int crossId = 0;

	public static void main(String[] args) throws IOException {
		peopleMap = new HashMap<Person, Integer>();
		if (args[0] == null) {
			throw new IllegalArgumentException("keyID cannot be null");
		}
		if (args[1] == null) {
			throw new IllegalArgumentException("keyID cannot be null");
		}
		keyId = args[0];
		secretKey = args[1];

		URL resource = ImportData.class.getClassLoader().getResource("filmy.csv");
		FileInputStream fis = new FileInputStream(resource.getFile());
		DataInputStream in = new DataInputStream(fis);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		movies = new HashMap<Integer, Movie>();
		br.readLine();
		while ((strLine = br.readLine()) != null) {
			movies.put(movies.size()+1, new Movie(strLine));
		}
		in.close();
		client = new AmazonSimpleDBClient(new BasicAWSCredentials(keyId, secretKey));
		//create domains
		client.createDomain(new CreateDomainRequest("movies"));
		client.createDomain(new CreateDomainRequest("people"));
		client.createDomain(new CreateDomainRequest("people_movies"));
		
		//fill movies
		BatchPutAttributesRequest batchPut = new BatchPutAttributesRequest();
		batchPut.setDomainName("movies");
		List<ReplaceableItem> items = new ArrayList<ReplaceableItem>();
		int counter = 0;
		for (Entry<Integer, Movie> moviePair : movies.entrySet()) {
			items.add(getItem(moviePair));
			counter++;
			if (counter>24){
				counter = 0;
				batchPut.setItems(items);
				client.batchPutAttributes(batchPut);
				items = new ArrayList<ReplaceableItem>();
			}
		}
		batchPut.setItems(items);
		client.batchPutAttributes(batchPut);
		
		//fill people
		BatchPutAttributesRequest batchPutPeople = new BatchPutAttributesRequest();
		batchPutPeople.setDomainName("people");
		List<ReplaceableItem> peopleItems = new ArrayList<ReplaceableItem>();
		counter = 0;
		for (Entry<Person, Integer> personPair : peopleMap.entrySet()){
			peopleItems.add(getPersonItem(personPair));
			counter++;
			if (counter>24){
				counter = 0;
				batchPutPeople.setItems(peopleItems);
				client.batchPutAttributes(batchPutPeople);
				peopleItems = new ArrayList<ReplaceableItem>();
			}
		}
		batchPutPeople.setItems(peopleItems);
		client.batchPutAttributes(batchPutPeople);
		
		//fill cross reference table
		BatchPutAttributesRequest batchPutCross = new BatchPutAttributesRequest();
		batchPutCross.setDomainName("people_movies");
		List<ReplaceableItem> crossItems = new ArrayList<ReplaceableItem>();
		counter = 0;
		for (Entry<Integer, Movie> moviePair : movies.entrySet()){
			for (Person person : moviePair.getValue().getCrew()){
				crossItems.add(getCrossItem(moviePair, person));
				counter++;
				if (counter>24){
					counter = 0;
					batchPutCross.setItems(crossItems);
					client.batchPutAttributes(batchPutCross);
					crossItems = new ArrayList<ReplaceableItem>();
				}
			}
		}
		batchPutCross.setItems(crossItems);
		client.batchPutAttributes(batchPutCross);
	}

	private static ReplaceableItem getCrossItem(Entry<Integer, Movie> moviePair, Person person) {
		ReplaceableItem item = new ReplaceableItem(Integer.toString(crossId++));
		List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>();
		attributes.add(new ReplaceableAttribute("movieId", Integer.toString(moviePair.getKey()), true));
		attributes.add(new ReplaceableAttribute("personId", Integer.toString(peopleMap.get(person)), true));
		item.setAttributes(attributes);
		return item;
	}

	private static ReplaceableItem getPersonItem(Entry<Person, Integer> personPair) {
		ReplaceableItem item = new ReplaceableItem(Integer.toString(personPair.getValue()));
		List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>();
		attributes.add(new ReplaceableAttribute("name", personPair.getKey().getName(), true));
		attributes.add(new ReplaceableAttribute("director", Boolean.toString(personPair.getKey().isDirector()), true));
		item.setAttributes(attributes);
		return item;
	}

	private static ReplaceableItem getItem(Entry<Integer, Movie> moviePair) {
		ReplaceableItem item = new ReplaceableItem(Integer.toString(moviePair.getKey()),
				getAttributes(moviePair.getValue()));
		return item;
	}

	private static List<ReplaceableAttribute> getAttributes(Movie movie) {
		List<ReplaceableAttribute> list = new ArrayList<ReplaceableAttribute>();
		list.add(new ReplaceableAttribute("title", movie.getTitle(), true));
		for (String studio : movie.getStudio()) {
			list.add(new ReplaceableAttribute("studio", studio, false));
		}
		for (String genre : movie.getGenre()) {
			list.add(new ReplaceableAttribute("genre", genre, false));
		}
		list.add(new ReplaceableAttribute("boxOffice", movie.getBoxOffice(), true));
		return list;
	}

	static class Movie {
		private String opening;
		private String title;
		private List<String> studio;
		private List<Person> crew;
		private List<String> genres;
		private String boxOffice;

		public Movie(String line) {
			String[] array = line.split(";");
			opening = array[0].trim() + " " + array[1].trim();
			title = array[2].trim();
			// studios
			String[] studiosArray = array[3].split("/");
			studio = new ArrayList<String>();
			for (String string : studiosArray) {
				studio.add(string.trim());
			}
			// crew
			String[] crewArray = array[4].split(",");
			crew = new ArrayList<Person>();
			for (String string : crewArray) {
				Person p = null;
				if (string.contains("(director)")) {
					p = new Person(string.substring(0, string.indexOf("(")).trim(), true);
				} else {
					p = new Person(string.trim(), false);
				}
				crew.add(p);
			}
			// genre
			String[] genresArray = array[5].split(",");
			genres = new ArrayList<String>();
			for (String genre : genresArray) {
				genres.add(genre.trim());
			}
			if (array.length < 7) {
				boxOffice = "";
			} else {
				boxOffice = array[6].trim();
			}

		}

		public String getOpening() {
			return opening;
		}

		public void setOpening(String opening) {
			this.opening = opening;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<String> getStudio() {
			return studio;
		}

		public void setStudio(List<String> studio) {
			this.studio = studio;
		}

		public List<Person> getCrew() {
			return crew;
		}

		public void setCrew(List<Person> person) {
			this.crew = person;
		}

		public String getBoxOffice() {
			return boxOffice;
		}

		public void setBoxOffice(String boxOffice) {
			this.boxOffice = boxOffice;
		}

		public List<String> getGenre() {
			return genres;
		}

		public void setGenre(List<String> genre) {
			this.genres = genre;
		}

		public String getCrewString() {
			StringBuffer buffer = new StringBuffer();
			for (Person person : crew) {
				buffer.append(person.toString());
				buffer.append("\n");
			}
			return buffer.toString();
		}

		public String getStudioString() {
			StringBuffer buffer = new StringBuffer();
			for (String stud : studio) {
				buffer.append(stud.toString());
				// buffer.append(", ");
			}
			return buffer.toString();
		}

		public String getGenreString() {
			StringBuffer buffer = new StringBuffer();
			for (String g : genres) {
				buffer.append(g.toString());
				buffer.append(", ");
			}
			return buffer.toString();
		}
	}

	static class Person {
		private String name;
		private boolean director;

		public Person(String name, boolean director) {
			this.name = name;
			this.director = director;
			if (!peopleMap.containsValue(this)) {
				peopleMap.put(this, peopleMap.size() + 1);
			}
		}

		public String getName() {
			return name;
		}

		public boolean isDirector() {
			return director;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setDirector(boolean director) {
			this.director = director;
		}

		@Override
		public String toString() {
			if (director) {
				return name + " DIRECTOR";
			} else {
				return name;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Person) {
				Person p = (Person) obj;
				return name.equalsIgnoreCase(p.getName());
			}
			return false;
		}
	}

}
