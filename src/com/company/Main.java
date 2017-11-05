package com.company;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import java.io.*;
import java.util.*;

public class Main {

    public static String collection_word="";
    public static void main(String[] args) throws IOException {

        PrintStream err = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);
        ArrayList<String> array_fields = new ArrayList<>();
        HashMap<String,ArrayList<String>> campi = field_table(array_fields,args[0]);
        //HashMap<String,ArrayList<String>> test = read_test(args[1]);

        DBCollection collection = mongo_initialization(br);
        campi.put(collection_word,new ArrayList<String>(Arrays.asList(collection_word+"/NN")));
        //DBCollection collection = null;
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        query_insertion:
        while(true){
            System.out.println("Insert your query:");
            String input_query = br.readLine();

            if(input_query.isEmpty() || input_query.trim().length()<1)
                continue;
            if(input_query.equals("exit"))
                System.exit(0);

            Annotation annotation = new Annotation(input_query);
            pipeline.annotate(annotation);
            Query q = new Query(input_query, annotation);

            QueryAnalyzer qa = new QueryAnalyzer(q);
            qa.t_group(campi);
            qa.t_sort(campi);

            char verb_flag = qa.t_searching_field(campi);
            if(verb_flag=='X')
                continue;

            qa.remove_tokens();
            qa.t_condition_field(campi);

            if (ambiguities_solver(br,q.searching_fields,'f')) continue;
            if (ambiguities_solver(br,q.grouping_clauses,'g')) continue;
            if (ambiguities_solver(br,q.sorting_clauses,'s')) continue;
            if (ambiguities_solver(br,q.condition_fields,'c')) continue;

            qa.query_type(verb_flag);
            qa.t_condition();
            MdbQueryBuilder qb = new MdbQueryBuilder(q,collection);

        }
    }

    private static HashMap<String, ArrayList<String>> read_test( String url){
        HashMap<String,ArrayList<String>> campi = new HashMap<>();
        String filename = url;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String sCurrentLine;
            String key="";
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.contains("MDBQ")){
                    key = sCurrentLine.split(":")[1];
                    campi.put(key,new ArrayList<>());
                }
                else{
                    campi.get(key).add(sCurrentLine.split(":")[1]);
                }
            }
        } catch (IOException e) {e.printStackTrace();}
        for(Map.Entry<String,ArrayList<String>> entry : campi.entrySet()){
            System.out.println(entry.getKey());
            for(String query : entry.getValue())
            System.out.println(query);
        }
        return campi;
    }

    private static HashMap<String, ArrayList<String>> field_table(ArrayList<String> array_fields, String url){
        HashMap<String,ArrayList<String>> campi = new HashMap<>();
        String filename = url;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.contains("=")){
                    String key = sCurrentLine.split("=")[0];
                    String values = sCurrentLine.split("=")[1];
                    if(!values.contains(",")){
                        ArrayList<String> as = new ArrayList<>();
                        as.add(values);
                        campi.put(key,as);
                    }
                    else{
                        String fiel[] = values.split(",");
                        ArrayList<String> as = new ArrayList<>();
                        Collections.addAll(as, fiel);
                        campi.put(key,as);
                    }
                }
                else{
                    if(sCurrentLine.compareTo("arrays")==0)
                        continue;
                    else{
                        array_fields.add(sCurrentLine);
                    }
                }
            }
        } catch (IOException e) {e.printStackTrace();}

        for(Map.Entry<String, ArrayList<String>> entry : campi.entrySet()) {
            ArrayList<String> value = entry.getValue();
            //System.out.print(entry.getKey() + " ------>  ");
            //for (String ss : value)
            //    System.out.print(ss + " ");
            //System.out.println();
        }
        return campi;
    }

    private static boolean ambiguities_solver(BufferedReader br, HashMap<List<IndexedWord>, List<String>> searching_fields, char type) {
        String type_of = "";
        switch(type){
            case 'f':
                type_of="searching fields: ";
                break;
            case 'g':
                type_of="grouping fields: ";
                break;
            case 's':
                type_of="sorting fields: ";
                break;
            case 'c':
                type_of="conditional fields: ";
                break;
        }
        for(Map.Entry<List<IndexedWord>,List<String>> entry : searching_fields.entrySet()){
            if(entry.getValue().size()>1){
                System.out.println("Check which of these possibilities is what you are looking for " + type_of);
                int p = 1;
                for(String s: entry.getValue()) {
                    if(s.contains("$"))
                        System.out.println(p +"- " + s.split("\\$")[1].split(":")[0] + " of field: " + s.split(":")[1]);
                    else
                        System.out.println(p +"- " + "field: " + s);
                    p++;
                }
                System.out.println("Digit an integer from 1 to " + entry.getValue().size());
                Integer parsed=0;
                try {
                    parsed = Integer.parseInt(br.readLine());
                }
                catch (IOException e){System.err.println("Error: " + e);}
                catch (NumberFormatException e) {System.err.println("Invalid number");}
                if(parsed<1 && parsed > entry.getValue().size()){
                    System.out.println("integer out of bound");
                    return true;
                }
                String chosen = entry.getValue().get(parsed-1);
                entry.getValue().clear();
                entry.getValue().add(chosen);
            }
        }
        return false;
    }

    public static DBCollection mongo_initialization(BufferedReader br) throws IOException {
        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );

        System.out.print("List of available database: ");
        for(String ss : mongoClient.listDatabaseNames())
            System.out.print(ss + " ");
        System.out.println();
        System.out.print("Type the name of the database to query: ");
        String data = br.readLine();
        System.out.println("Database chosen: " + data);
        DB database = mongoClient.getDB(data);

        System.out.print("List of available collection: ");
        for(String tp : database.getCollectionNames())
            System.out.print(tp + " ");
        System.out.println();
        System.out.print("Type the name of the collection to query: ");
        data = br.readLine();
        System.out.println("Collection chosen: " + data);
        DBCollection collection = database.getCollection(data);

        System.out.print("Type the keyword that you'd like to use to refer to documents of collection: ");
        data = br.readLine();
        System.out.println("Keyword chosen: " + data);
        collection_word = data;
        System.out.println("This is a an example document for the collection");
        DBObject c=collection.findOne();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(c);
        System.out.println(json);

        return collection;
    }
}
