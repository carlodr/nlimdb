package com.company;


import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations;
import edu.stanford.nlp.util.Index;

import java.util.*;

public class MdbQueryBuilder {

    private LinkedHashMap<String,LinkedHashSet<String>> patterns;
    private Query query;

    public MdbQueryBuilder(Query q, DBCollection collection){
        query = q;
        String s = find_pattern(query);
        String p = "";
        if(query.conditions.size()>0 && query.sorting_clauses.size()>0){
            p = s + ", whith list of stage: " + patterns.get(s).toString();
            System.out.println(p);
        }
        if(query.conditions.size()>0 && query.sorting_clauses.size()==0){
            p = p = s + ", whith list of stage: " + patterns.get(s).toString();
            p = p.replace(", sort","");
            System.out.println(p);
        }
        if(query.conditions.size()==0 && query.sorting_clauses.size()>0){
            p = s + ", whith list of stage: " + patterns.get(s).toString();
            p = p.replace("match, ","");
            System.out.println(p);
        }
        if(query.conditions.size()==0 && query.sorting_clauses.size()==0){
            p = s + ", whith list of stage: " + patterns.get(s).toString();
            p = p.replace(", sort","");
            p = p.replace("match, ","");
            System.out.println(p);
        }

        String query="";
        if(s.compareTo("Retrieve")==0){
            query ="[";
        if(p.contains("match")){
            query=query+this.match(q.conditions,q.words_to_stem);
            query = query+","+this.project(q.searching_fields);
        }
        else
            query = query+this.project(q.searching_fields);
            if(p.contains("sort"))
            query=query+this.sort(q.sorting_clauses);
        query = query +"]";
        }
        else if(s.compareTo("Retrieve&Calculate")==0){
            query ="[";
            if(p.contains("match")){
                query=query+this.match(q.conditions,q.words_to_stem);
                query = query+","+this.group(s);
            }
            else
                query = query+this.group(s);
            if(p.contains("sort"))
                query=query+this.sort(q.sorting_clauses);
            query = query +"]";
        }
        else if(s.compareTo("RetrieveGroup&Calculate")==0){
            query ="[";
            if(p.contains("match")){
                query=query+this.match(q.conditions,q.words_to_stem);
                query = query+","+this.group(s);
            }
            else
                query = query+this.group(s);
            if(p.contains("sort"))
                query=query+this.sort(q.sorting_clauses);
            query = query +"]";
        }

        System.out.println(query);
        List<DBObject> mbdquery= (List<DBObject>) JSON.parse(query);
        Iterable<DBObject> result=collection.aggregate(mbdquery).results();
        for(DBObject c : result)
            System.out.println(c);
    }

    private Condition find_logic(HashMap<Condition,Set<IndexedWord>> cs, IndexedWord w){
        for(Map.Entry<Condition,Set<IndexedWord>> cp : cs.entrySet()) {
            //System.out.println("search: " + w.value() + " in: " + cp.getValue().toString());
            if (cp.getValue().contains(w)){
                //System.out.println("finded: " + w.value() + " in: " + cp.getValue().toString());
                return cp.getKey();
            }
            for(IndexedWord ww : cp.getValue()){
                if(query.graph.getEdge(ww,w)!=null && !query.graph.getEdge(ww,w).getRelation().equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !query.graph.getEdge(ww,w).getRelation().equals(UniversalEnglishGrammaticalRelations.getConj("or"))){
                    //System.out.println("finded: " + w.value() + " in: " + query.graph.getEdge(ww,w).toString());
                    return cp.getKey();
                }
                if(query.graph.getEdge(w,ww)!=null && !query.graph.getEdge(w,ww).getRelation().equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !query.graph.getEdge(w,ww).getRelation().equals(UniversalEnglishGrammaticalRelations.getConj("or"))){
                    //System.out.println("finded: " + w.value() + " in: " + query.graph.getEdge(w,ww).toString());
                    return cp.getKey();
                }
            }
        }
        return null;
    }

    private String match(List<Condition> cond_list,HashMap<IndexedWord,String> words_to_stem) {
        HashMap<Condition,Set<IndexedWord>> cs = new HashMap<>();
        for(Condition c : cond_list){
            Set<IndexedWord> st = new HashSet<>();
            for(Map.Entry<Set<IndexedWord>, Set<IndexedWord>> entry : c.condition.entrySet()){
                st.addAll(entry.getKey());
                st.addAll(entry.getValue());
            }
            st.addAll(c.field);
            cs.put(c,st);
        }
        //System.out.println(cs.toString());
        String match_string = "{$match:{";
        String cc = "";
        if(cond_list.size()==2){

            List<SemanticGraphEdge> ar = query.graph.findAllRelns(UniversalEnglishGrammaticalRelations.getConj("and"));
            if(ar!=null){
                //System.out.println(ar.toString());
                for(SemanticGraphEdge eg : ar){
                    Set<Condition> sc = new HashSet<>();
                    Condition c1 = find_logic(cs,eg.getGovernor());
                    Condition c2 = find_logic(cs,eg.getDependent());
                    sc.add(c1);
                    sc.add(c2);
                    if(c1 != null && c2!=null && !(c1.equals(c2))){
                        cc = "$and";
                    }
                }
            }
            List<SemanticGraphEdge> or = query.graph.findAllRelns(UniversalEnglishGrammaticalRelations.getConj("or"));
            if(or!=null){
                for(SemanticGraphEdge eg : or){
                    Set<Condition> sc = new HashSet<>();
                    Condition c1 = find_logic(cs,eg.getGovernor());
                    Condition c2 = find_logic(cs,eg.getDependent());
                    sc.add(c1);
                    sc.add(c2);
                    if(c1!=null && c2!=null && !(c1.equals(c2))){
                        cc="$or";
                        match_string = match_string+"$or:[";
                    }
                }
            }
            System.out.println(cc.toString());
        }

        for (Condition c : cond_list){
            String ops = "";
            String value = "";
            HashMap<HashMap<Set<IndexedWord>, Set<IndexedWord>>, String> logical = new HashMap<>();
            logical.putAll(c.getLogical());
            for (Map.Entry<HashMap<Set<IndexedWord>, Set<IndexedWord>>, String> entry : logical.entrySet())
                if(entry.getValue().equals("-") && entry.getKey().size()==1) {
                if(cc.compareTo("$or")==0)
                    match_string = match_string + "{" + c.string_representation.get(0) + ":{";
                else
                    match_string = match_string + c.string_representation.get(0) + ":{";
                    for (Map.Entry<Set<IndexedWord>, Set<IndexedWord>> ent : entry.getKey().entrySet()) {
                        match_string = match_string + operator(ent.getValue()) + ":";
                        for (IndexedWord x : ent.getKey()) {
                            value = value + words_to_stem.get(x).split("/")[0];
                        }
                    }
                    try {
                        Integer parsed = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        value = "'" + value + "'";
                    }
                    if(cc.compareTo("$or")==0)
                        match_string = match_string + value + "}},";
                    else
                        match_string = match_string + value + "},";
                }
                else{
                    if (entry.getKey().size()==2){
                        if(entry.getValue().equals("or")){
                            if(cc.compareTo("$or")==0)
                                ops = "{$or:[";
                            else
                                ops = "$or:[";
                        }
                        if(entry.getValue().equals("and")){
                            if(cc.compareTo("$or")==0)
                                ops = "{$and:[";
                            else
                                ops = "$and:[";
                        }
                        for (Map.Entry<Set<IndexedWord>, Set<IndexedWord>> ent : entry.getKey().entrySet()) {
                            String pops = "";
                            value = "";
                            pops = "{" + c.string_representation.get(0) + ":{";
                            pops = pops + operator(ent.getValue()) + ":";
                            for (IndexedWord x : ent.getKey()){
                                value = value + words_to_stem.get(x).split("/")[0];
                            }
                            try {
                                Integer parsed = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                value = "'" + value + "'";
                            }
                            ops = ops + pops + value + "}},";
                        }
                    }
                    if(cc.compareTo("$or")==0)
                        match_string = match_string + ops.substring(0,ops.length()-1) + "]},";
                    else
                        match_string = match_string + ops.substring(0,ops.length()-1) + "],";
                }
        }
        if(cc.compareTo("$or")==0)
            match_string = match_string.substring(0,match_string.length()-1) +"]}}";
        else
            match_string = match_string.substring(0,match_string.length()-1) +"}}";
        return match_string;
    }

    private String project(HashMap<List<IndexedWord>,List<String>> searching_fields){
        String project_fields="{$project:{_id:0,";
        for(Map.Entry<List<IndexedWord>,List<String>> entry : searching_fields.entrySet()){
            project_fields = project_fields+entry.getValue().get(0)+":1,";
        }
        project_fields = project_fields.substring(0,project_fields.length()-1);
        return project_fields + "}}";
    }

    private String sort(HashMap<List<IndexedWord>,List<String>> sorting_clauses){
        String project_fields=",{$sort:{";
        for(Map.Entry<List<IndexedWord>,List<String>> entry : sorting_clauses.entrySet()){
            project_fields = project_fields+entry.getValue().get(0)+":1,";
        }
        project_fields = project_fields.substring(0,project_fields.length()-1);
        return project_fields + "}}";
    }
    private String group(String pattern){
        String group_fields="{$group:{_id:";
        if(pattern.compareTo("Retrieve&Calculate")==0){
            group_fields= group_fields+"null,";
            for(Map.Entry<List<IndexedWord>,List<String>> entry : query.searching_fields.entrySet()){
                String s = entry.getValue().get(0);
                String operator = "";
                String field_name="";
                if(s.contains("$")){
                    operator = s.split(":")[0].replace("$","");
                    field_name=s.replace(":","");
                    field_name=field_name.replace("$","");
                    if(operator.compareTo("count")!=0)
                        group_fields = group_fields+field_name+":{"+aggregator(operator)+":"+"'$"+s.split(":")[1]+"'}";
                    else
                        group_fields = group_fields+field_name+":{"+aggregator(operator)+"}";
                }
            }
        }
        else if(pattern.compareTo("RetrieveGroup&Calculate")==0){
            if(query.grouping_clauses.size()==1){
                for(Map.Entry<List<IndexedWord>,List<String>> ent : query.grouping_clauses.entrySet()) {
                    group_fields = group_fields + "'$"+ent.getValue().get(0) + "',";
                }
            }
            else{
                group_fields=group_fields+"{";
                for(Map.Entry<List<IndexedWord>,List<String>> ent : query.grouping_clauses.entrySet()) {
                    group_fields = group_fields + ent.getValue().get(0) + ": '$"+ent.getValue().get(0)+"',";
                }
                group_fields=group_fields.substring(0,group_fields.length()-1)+"},";
            }
            for(Map.Entry<List<IndexedWord>,List<String>> entry : query.searching_fields.entrySet()){
                String s = entry.getValue().get(0);
                String operator = "";
                String field_name="";
                if(s.contains("$")){
                    operator = s.split(":")[0].replace("$","");
                    field_name=s.replace(":","");
                    field_name=field_name.replace("$","");
                    if(operator.compareTo("count")!=0)
                        group_fields = group_fields+field_name+":{"+aggregator(operator)+":"+"'$"+s.split(":")[1]+"'}";
                    else
                        group_fields = group_fields+field_name+":{"+aggregator(operator)+"}";
                }
            }
        }
        return group_fields + "}}";
    }

    private String unwind(HashMap<List<IndexedWord>,List<String>> searching_fields){
        String project_fields="{$project:{_id:0,";
        for(Map.Entry<List<IndexedWord>,List<String>> entry : searching_fields.entrySet()){
            project_fields = project_fields+entry.getValue().get(0)+":1,";
        }
        project_fields = project_fields.substring(0,project_fields.length()-1);
        return project_fields + "}}";
    }

    private static String aggregator(String op){
        HashMap<String,String> aggregator_map = new HashMap<>();
        aggregator_map.put("average","$avg");
        aggregator_map.put("sum","$sum");
        aggregator_map.put("count","$sum:1");
        aggregator_map.put("maximum","$max");
        aggregator_map.put("minimum","$min");
        aggregator_map.put("first","$first");
        aggregator_map.put("last","$last");
        aggregator_map.put("set","$addToSet");
        return aggregator_map.get(op);
    }

    private static String operator(Set<IndexedWord> op){
        String ops="";
        boolean negated = false;
        if(op.size()>1){
            for(IndexedWord w : op)
                if(w.value().compareTo("not")==0)
                    negated=true;
        }
        HashMap<String,String[]> aggregator_map = new HashMap<>();
        aggregator_map.put("$gt",new String[]{"greater","more","larger","higher","bigger","after","least"});
        aggregator_map.put("$lt",new String[]{"less","lesser","lower","minor","before","most"});
        aggregator_map.put("$eq",new String[]{"equal","equivalent","correspondent"});

        HashMap<String,String> neg_aggregator_map = new HashMap<>();
        neg_aggregator_map.put("$gt","$lte");
        neg_aggregator_map.put("$lt","$gtr");
        neg_aggregator_map.put("$eq","$ne");

        for(IndexedWord w : op) {
            for(Map.Entry<String,String[]> entry : aggregator_map.entrySet()){
                for(String s : entry.getValue())
                    if(w.value().compareTo(s)==0){
                        ops = entry.getKey();
                        break;
                    }
            }
        }
        if(negated)
            ops = neg_aggregator_map.get(ops);

        return ops;
    }

    private String find_pattern(Query q){
        patterns = new LinkedHashMap<>();
        patterns.put("Retrieve",new LinkedHashSet<>(Arrays.asList("match","project","sort")));
        patterns.put("Retrieve&Group",new LinkedHashSet<>(Arrays.asList("match","group","sort")));
        patterns.put("Retrieve&Calculate",new LinkedHashSet<>(Arrays.asList("match","group","sort")));
        patterns.put("RetrieveGroup&Calculate",new LinkedHashSet<>(Arrays.asList("match","group","sort")));
        patterns.put("GroupCalculate&Retrieve",new LinkedHashSet<>(Arrays.asList("group","match","sort")));
        patterns.put("Retrieve&NestedCalculate",new LinkedHashSet<>(Arrays.asList("match","project","sort")));
        int fields = 0;
        int condit = 0;
        int ff = 0;
        if(q.grouping_clauses.size()==0){
            for(Map.Entry<List<IndexedWord>,List<String>> entry : q.searching_fields.entrySet())
                if(!entry.getValue().get(0).contains("$") || (entry.getValue().get(0).contains("$") && (entry.getValue().get(0).contains("$year") || entry.getValue().get(0).contains("$month") || entry.getValue().get(0).contains("$week"))))
                    fields++;
            if(fields!=q.searching_fields.size())
                return "Retrieve&Calculate";
            else{
                for(Condition condition : q.conditions)
                    if(!condition.string_representation.get(0).contains("$") || (condition.string_representation.get(0).contains("$") && (condition.string_representation.get(0).contains("$year") || condition.string_representation.get(0).contains("$month") || condition.string_representation.get(0).contains("$week"))))
                        condit++;
                if(condit==q.conditions.size())
                    return "Retrieve";
                else
                    return "GroupCalculate&Retrieve";
            }
        }
        else{
            for(Map.Entry<List<IndexedWord>,List<String>> entry : q.searching_fields.entrySet())
                if(!entry.getValue().get(0).contains("$")|| (entry.getValue().get(0).contains("$") && (entry.getValue().get(0).contains("$year") || entry.getValue().get(0).contains("$month") || entry.getValue().get(0).contains("$week"))))
                    ff++;
            if(ff!=q.searching_fields.size())
                return "RetrieveGroup&Calculate";
            else
                return "Retrieve&Group";
        }
    }

    private boolean check_t_group(Query q){
        return true;
    }

    private boolean check_t_field(Query q){
        return true;
    }

}
