package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class PackageManager {
	private final static String ALL_PACKAGES_FILE = "all_packages.json";
	private final static String DEPENDENCIES_FILE = "dependencies.json";
	private final static String MODULES_DIR = "installed_modules";
	
	public void installDependencies() {
		JsonNode packagesTree = getPackagesTree();
		if (packagesTree != null) {
			Set<String> dependencies = getDependencies();
			if(dependencies != null){				
				dependencies.forEach((pack) -> {			
					installPackageFormTree(packagesTree, pack);
				});
				System.out.println("All done.");
			}
		}
		
	}
	
	private JsonNode getPackagesTree() {
		JsonFactory jsonFactory = new JsonFactory();
		JsonParser jp;
		try {
			jp = jsonFactory.createJsonParser(new File(ALL_PACKAGES_FILE));
			jp.setCodec(new ObjectMapper());
			return jp.readValueAsTree();
		} catch (JsonParseException jpe){
			System.out.println("Error parsing all_packages.json. " + jpe.toString());
		} catch (IOException ioe) {
			System.out.println("Error." + ioe.toString());
		}
		
		return null;
		
	}
	
	private Set<String> getDependencies(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, true);
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Dependencies dep;
		try {
			dep = mapper.readValue(new FileInputStream(DEPENDENCIES_FILE), Dependencies.class );
			return dep.getDependencies();
		} catch (JsonParseException jpe) {
			System.out.println("Error parsing " + DEPENDENCIES_FILE + ". " + jpe.toString());
		} catch (FileNotFoundException fnfe) {
			System.out.println("Missing file: " + DEPENDENCIES_FILE + "");
		} catch (IOException ioe) {
			System.out.println("Error." + ioe.toString());
		}
		
		return null;
	}
	
	private void installPackageFormTree(JsonNode jsonNode, String pack) {
		Iterator<Map.Entry<String, JsonNode>> ite = jsonNode.getFields();
		Set<String> installedModules = getInstalledModules();
		while(ite.hasNext()){
			Map.Entry<String, JsonNode> entry = ite.next();
			if (entry.getKey().equals(pack) && !installedModules.contains(pack)){
				System.out.println("Installing " + pack + ".");
				boolean success = (new File(MODULES_DIR + "/"+ pack + "/")).mkdirs();
				if (!success) {
				    System.out.println("Could not install " + pack + " aborting.");
				    return;
				}
				if (entry.getValue().isArray()) {
					int numElements = entry.getValue().size();
					if (numElements > 0){
						System.out.print("In order to install " + pack + ", we need ");
						for (int i = 0; i < numElements; i++) {
							String dependencyPack = entry.getValue().get(i).asText();
							System.out.print(dependencyPack);
							if (installedModules.contains(dependencyPack)) {
								System.out.print(" (" + dependencyPack + " is allready installed)");	
							}
							if (i < numElements - 1) {
								System.out.print(" and ");							
							}
						}
						System.out.println(".");
					}
					entry.getValue().forEach((val) -> {
						installPackageFormTree(jsonNode, val.asText());
					});
				} else {
					System.out.println(entry.getKey() + " must be an array.");
				}
			}
		}
	}
	
	private Set<String> getInstalledModules(){
		File[] directories = new File(MODULES_DIR + "/").listFiles(File::isDirectory);
		Set<String> dirs = new HashSet<>();
		
		if (directories != null){
			for (File dir : directories) {
				dirs.add(dir.getName());
			}
		}
		
		return dirs;
	}
}
