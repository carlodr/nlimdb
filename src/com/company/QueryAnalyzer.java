package com.company;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.company.Mapper.aggregator_mapping;
import static com.company.Mapper.field_mapping;
import static com.company.Mapper.similarity;

public class QueryAnalyzer {

    public Query query;

    public QueryAnalyzer(Query q){
        query = q;
        set_negation_list();
        tokenize_query();
    }

    private void tokenize_query(){
        List<CoreLabel> listed_sentence = query.sentence.get(CoreAnnotations.TokensAnnotation.class);
        SemanticGraph graph = query.sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        HashMap<IndexedWord,String> words_to_stem = new HashMap<>();
        HashSet<IndexedWord> stems_indexed = new HashSet<>();
        for(SemanticGraphEdge sg : graph.edgeListSorted()){
            stems_indexed.add(sg.getGovernor());
            stems_indexed.add(sg.getDependent());
        }

        int token_index= 0;
        for(IndexedWord word : stems_indexed){
            words_to_stem.put(word,listed_sentence.get(token_index).word()+"/"+listed_sentence.get(token_index).tag());
            token_index++;
        }
        query.words_to_stem.putAll(words_to_stem);
        System.out.println(query.graph.toString(SemanticGraph.OutputFormat.LIST));
    }

    private void set_negation_list() {
        query.negation=new HashMap<>();
        for(SemanticGraphEdge sgraph : query.graph.findAllRelns(UniversalEnglishGrammaticalRelations.NEGATION_MODIFIER)){
            query.negation.put(sgraph.getGovernor(),sgraph);
        }
    }

    public void t_sort(HashMap<String, ArrayList<String>> campi){
        SemanticGraph graph = query.graph;
        Set<IndexedWord> edges_to_remove = query.index_for_edge_to_remove;
        HashMap<List<IndexedWord>,List<String>> abc = new HashMap<>();
        ILexicalDatabase db = new NictWordNet();
        WS4JConfiguration.getInstance().setMFS(true);
        for(GrammaticalRelation uv : UniversalEnglishGrammaticalRelations.getNmods()){
            for(SemanticGraphEdge sge : graph.findAllRelns(uv)){
                if(sge.getGovernor().toString().split("/")[1].charAt(0) == 'V')
                    if(similarity(db,sge.getGovernor().value(),"sort",'V')>0.94){
                        HashMap<List<IndexedWord>,List<String>> tempabc = new HashMap<>();
                        ArrayList<IndexedWord> searched_indexed = new ArrayList<>();
                        for(IndexedWord id : graph.getChildrenWithRelns(sge.getDependent(),Arrays.asList(UniversalEnglishGrammaticalRelations.ADJECTIVAL_MODIFIER,UniversalEnglishGrammaticalRelations.COMPOUND_MODIFIER, UniversalEnglishGrammaticalRelations.getNmod("of")))){
                            if(id.value().compareTo("descend")!=0 && id.value().compareTo("ascend")!=0)
                                searched_indexed.add(id);
                        }
                        searched_indexed.add(sge.getDependent());
                        tempabc.put(searched_indexed,new ArrayList<>());
                        edges_to_remove.add(sge.getGovernor());
                        for(IndexedWord ixd : searched_indexed){
                            edges_to_remove.add(ixd);
                        }
                        field_mapping(campi,tempabc);
                        String order="";
                        for(SemanticGraphEdge edg : graph.getOutEdgesSorted(sge.getDependent())) {
                            if(similarity(db,edg.getDependent().value(),"ascend",'V')>0.94)
                                order="ascending";
                            if(similarity(db,edg.getDependent().value(),"descend",'V')>0.94)
                                order="descending";
                        }
                        for(Map.Entry<List<IndexedWord>,List<String>> entry : tempabc.entrySet())
                            if(entry.getValue().size()>0 && !order.isEmpty()){
                                ArrayList<String> fieldsreplace = new ArrayList<>();
                                ArrayList<String> fieldsadd = new ArrayList<>();
                                for(String field : entry.getValue()){
                                    fieldsadd.add(field+"+"+order);
                                    fieldsreplace.add(field);
                                }
                                tempabc.get(entry.getKey()).removeAll(fieldsreplace);
                                tempabc.get(entry.getKey()).addAll(fieldsadd);
                            }
                        abc.putAll(tempabc);
                    }
            }
        }
        /*for(Map.Entry<List<IndexedWord>, List<String>> ent : abc.entrySet()){
            if(ent.getValue().size()>0)
                System.out.println("clausola di sort per " + ent.getKey() + " : " + ent.getValue().toString());
        }*/
        query.sorting_clauses.putAll(abc);
    }

    public void t_group(HashMap<String, ArrayList<String>> campi){
        SemanticGraph graph = query.graph;
        Set<IndexedWord> edges_to_remove = query.index_for_edge_to_remove;
        HashMap<List<IndexedWord>,List<String>> abc = new HashMap<>();
        ILexicalDatabase db = new NictWordNet();
        WS4JConfiguration.getInstance().setMFS(true);
        for(GrammaticalRelation uv : UniversalEnglishGrammaticalRelations.getNmods()){
            for(SemanticGraphEdge sge : graph.findAllRelns(uv)){
                if(sge.getGovernor().toString().split("/")[1].charAt(0) == 'V')
                    if(similarity(db,sge.getGovernor().value(),"group",'V')>0.94 || similarity(db,sge.getGovernor().value(),"find",'V')>0.94 || similarity(db,sge.getGovernor().value(),"enumerate",'V')>0.94){
                        HashMap<List<IndexedWord>,List<String>> tempabc = new HashMap<>();
                        ArrayList<IndexedWord> searched_indexed = new ArrayList<>();
                        for(IndexedWord id : graph.getChildrenWithRelns(sge.getDependent(),Arrays.asList(UniversalEnglishGrammaticalRelations.ADJECTIVAL_MODIFIER,UniversalEnglishGrammaticalRelations.COMPOUND_MODIFIER, UniversalEnglishGrammaticalRelations.getNmod("of"))))
                            searched_indexed.add(id);
                        searched_indexed.add(sge.getDependent());
                        tempabc.put(searched_indexed,new ArrayList<>());
                        edges_to_remove.add(sge.getGovernor());
                        for(IndexedWord ixd : searched_indexed){
                            edges_to_remove.add(ixd);
                        }
                        field_mapping(campi,tempabc);
                        abc.putAll(tempabc);
                    }
            }
        }
        /*for(Map.Entry<List<IndexedWord>, List<String>> ent : abc.entrySet()){
            if(ent.getValue().size()>0)
                System.out.println("clausola di group per " + ent.getKey() + " : " + ent.getValue().toString());
        }*/
        query.grouping_clauses.putAll(abc);
    }

    public char t_searching_field(HashMap<String, ArrayList<String>> campi) throws IOException {
        char verb_flag = 'X';
        HashMap<IndexedWord, String> words_to_stem = query.words_to_stem;
        boolean how_many = howmany_finder(words_to_stem);
        IndexedWord query_verb = null;
        for(Map.Entry<IndexedWord,String> entry : words_to_stem.entrySet()){
            if(entry.getKey().tag().contains("VB")){
                query_verb = entry.getKey();
                ILexicalDatabase db = new NictWordNet();
                if(similarity(db,entry.getKey().value(),"count",'V')>0.89 || howmany_finder(words_to_stem))
                    verb_flag = 'C';
                else if(similarity(db,entry.getKey().value(),"find",'V')>0.89 || similarity(db,entry.getKey().value(),"return",'V')>0.89)
                    verb_flag = 'R';
                else{
                    System.out.println("Not a query verb");
                    return 'X';
                }
                query.index_for_edge_to_remove.add(query_verb);
                break;
            }
        }

        if(query_verb == null){
            System.out.println("No verbs founded");
            return 'X';
        }

        //list of direct_object, possible fields for the query form find/count the...
        boolean query_direct_obj = false;
        boolean query_wh_be_obj = false;
        boolean query_wh_obj = false;

        for(IndexedWord word : query.graph.getChildrenWithReln(query_verb, UniversalEnglishGrammaticalRelations.DIRECT_OBJECT)){
            Set<IndexedWord> list_of_related = related_words(word, query.graph,query.index_for_edge_to_remove);
            query.index_for_edge_to_remove.add(word);
            query.searching_fields.put(new ArrayList<>(list_of_related),new ArrayList<>());
            query_direct_obj = true;
        }
        if(!query_direct_obj){
            //System.out.println("dentro");
            for(IndexedWord word : query.graph.getChildrenWithReln(query_verb, UniversalEnglishGrammaticalRelations.CLAUSAL_COMPLEMENT)){
                Set<IndexedWord> list_of_related = null;
                if(word.tag().contains("VB")){
                    for(IndexedWord ixw : query.graph.getChildrenWithRelns(word, Arrays.asList(UniversalEnglishGrammaticalRelations.NOMINAL_SUBJECT,UniversalEnglishGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT))){
                        query.index_for_edge_to_remove.add(ixw);
                        list_of_related = related_words(ixw, query.graph,query.index_for_edge_to_remove);
                        query.searching_fields.put(new ArrayList<>(list_of_related),new ArrayList<>());
                        query_wh_obj = true;
                    }
                }
                else{
                    list_of_related = related_words(word, query.graph,query.index_for_edge_to_remove);
                    query_wh_be_obj = true;
                    query.index_for_edge_to_remove.add(word);
                }
                query.searching_fields.put(new ArrayList<>(list_of_related),new ArrayList<>());
            }
        }

        //System.out.println("pppp +     " + searching_fields.toString());

        if(how_many)
            for(Map.Entry<IndexedWord,String> entry : words_to_stem.entrySet()){
                if(entry.getValue().compareTo("how/WRB")==0 || entry.getValue().compareTo("many/JJ")==0)
                    query.index_for_edge_to_remove.add(entry.getKey());
            }
        field_mapping(campi,query.searching_fields);
        aggregator_mapping(campi,query.searching_fields);
        return verb_flag;
    }

    public void remove_tokens(){
        Set<SemanticGraphEdge> edges_to_rm = new HashSet<>();
        for(IndexedWord torm : query.index_for_edge_to_remove){
            edges_to_rm.addAll(query.graph.getIncomingEdgesSorted(torm));
            edges_to_rm.addAll(query.graph.getOutEdgesSorted(torm));
            for (SemanticGraphEdge gr : edges_to_rm)
                query.graph.removeEdge(gr);
        }
        System.out.println(query.graph.toString(SemanticGraph.OutputFormat.LIST));

        query.index_for_edge_to_remove.clear();
        HashSet<IndexedWord> listn = new HashSet<>();
        for(SemanticGraphEdge sg : query.graph.edgeListSorted()) {
            if(sg.getGovernor().toString().split("/")[1].charAt(0)=='V' || sg.getGovernor().toString().split("/")[1].charAt(0)=='N'|| sg.getGovernor().toString().split("/")[1].charAt(0)=='J' || sg.getGovernor().tag().contains("CD") || sg.getGovernor().toString().split("/")[1].contains("IN"))
                listn.add(sg.getGovernor());
            if(sg.getDependent().toString().split("/")[1].charAt(0)=='V' || sg.getDependent().toString().split("/")[1].charAt(0)=='N' || sg.getDependent().toString().split("/")[1].charAt(0)=='J' || sg.getDependent().tag().contains("CD") || sg.getDependent().toString().split("/")[1].contains("IN"))
                listn.add(sg.getDependent());
        }
        query.token_list.addAll(listn);
    }

    public void t_condition_field(HashMap<String, ArrayList<String>> campi) throws IOException {
        HashSet<IndexedWord> removeix = new HashSet<>();

        HashSet<IndexedWord> listaggre = new HashSet<>();
        for(IndexedWord ppp : query.token_list){
            if(Mapper.is_aggregator(ppp.value()))
                listaggre.add(ppp);
        }

        //System.out.println(listaggre.toString());
        query.token_list.removeAll(listaggre);
        //System.out.println(listn.toString());

        HashMap<List<IndexedWord>, List<String>> aaa = new HashMap<>();

        for(IndexedWord pps : listaggre){
            Set<IndexedWord> idlist = new HashSet<>();
            if(pps.tag().charAt(0)=='J')
                for(IndexedWord indexedge : query.graph.getParentsWithReln(pps,UniversalEnglishGrammaticalRelations.ADJECTIVAL_MODIFIER)){
                    idlist.addAll(related_words(indexedge,query.graph,removeix));
                    removeix.add(indexedge);
                    aaa.put(new ArrayList<>(idlist),new ArrayList<>());
                }
            if(pps.tag().charAt(0)=='N'){
                boolean composedof = false;
                for(IndexedWord indexedge : query.graph.getChildrenWithReln(pps,UniversalEnglishGrammaticalRelations.getNmod("of"))){
                    idlist.add(pps);
                    removeix.add(pps);
                    removeix.add(indexedge);
                    idlist.addAll(related_words(indexedge,query.graph,removeix));
                    aaa.put(new ArrayList<>(idlist),new ArrayList<>());
                    composedof= true;
                }
                if(!composedof){
                    for(IndexedWord indexedge : query.graph.getParentsWithReln(pps,UniversalEnglishGrammaticalRelations.COMPOUND_MODIFIER)){
                        idlist.addAll(related_words(indexedge,query.graph,removeix));
                        removeix.add(indexedge);
                        aaa.put(new ArrayList<>(idlist),new ArrayList<>());
                        //System.out.println(idlist.toString());
                    }}
            }
        }

        query.token_list.removeAll(removeix);

        HashMap<List<IndexedWord>,List<String>> tempmap = new HashMap<>();
        removeix.clear();
        HashSet<IndexedWord> tempremove = new HashSet<>();
        for(IndexedWord pps : query.token_list){
            if(removeix.contains(pps))
                continue;
            Set<IndexedWord> idlist = new HashSet<>();
            if(pps.tag().charAt(0)=='N'){
                boolean composedof = false;
                boolean listed = false;
                for(IndexedWord indexedge : query.graph.getChildrenWithReln(pps,UniversalEnglishGrammaticalRelations.getNmod("of"))){
                    idlist.add(pps);
                    idlist.addAll(related_words(indexedge,query.graph,tempremove));
                    tempremove.add(pps);
                    tempremove.add(indexedge);
                    tempmap.put(new ArrayList<>(idlist),new ArrayList<>());
                    field_mapping(campi,tempmap);
                    if(tempmap.get(new ArrayList<>(idlist)).size()>0){
                        removeix.addAll(tempremove);
                        aaa.put(new ArrayList<>(idlist),new ArrayList<>());
                        composedof= true;
                    }
                    tempmap.clear();
                    tempremove.clear();
                    idlist.clear();
                }
                if(!composedof)
                    for(IndexedWord indexedge : query.graph.getParentsWithReln(pps,UniversalEnglishGrammaticalRelations.COMPOUND_MODIFIER)){
                        idlist.addAll(related_words(indexedge,query.graph,tempremove));
                        tempremove.add(indexedge);
                        tempmap.put(new ArrayList<>(idlist),new ArrayList<>());
                        field_mapping(campi,tempmap);
                        if(tempmap.get(new ArrayList<>(idlist)).size()>0){
                            removeix.addAll(tempremove);
                            aaa.put(new ArrayList<>(idlist),new ArrayList<>());
                        }
                        tempmap.clear();
                        tempremove.clear();
                        idlist.clear();
                        listed = true;
                    }
                if(!listed && !composedof){
                    tempmap.put(new ArrayList<>(Collections.singletonList(pps)),new ArrayList<>());
                    field_mapping(campi,tempmap);
                    if(tempmap.get(new ArrayList<>(Collections.singletonList(pps))).size()>0){
                        removeix.add(pps);
                        aaa.put(new ArrayList<>(Collections.singletonList(pps)),new ArrayList<>());
                    }
                    tempmap.clear();
                }
            }
            if(pps.tag().charAt(0)=='V'){
                if(pps.value().compareTo("have")==0 || pps.value().compareTo("be")==0)
                    continue;
                tempmap.put(new ArrayList<>(Collections.singletonList(pps)),new ArrayList<>());
                field_mapping(campi,tempmap);
                if(tempmap.get(new ArrayList<>(Collections.singletonList(pps))).size()>0){
                    removeix.add(pps);
                    aaa.put(new ArrayList<>(Collections.singletonList(pps)),new ArrayList<>());
                }
                tempmap.clear();
            }
        }
        query.token_list.removeAll(removeix);
        //System.out.println(listn.toString());
        query.condition_fields.putAll(aaa);
        field_mapping(campi,query.condition_fields);
        aggregator_mapping(campi,query.condition_fields);
    }

    public void t_condition(){
        List<IndexedWord> operators = new ArrayList<>();
        for(IndexedWord word: query.token_list){
            if(Mapper.is_operator(word.value()))
                operators.add(word);
        }
        query.token_list.removeAll(operators);
        //System.out.println(listn.toString());
        //System.out.println(operators.toString());

        HashMap<IndexedWord,List<IndexedWord>> mapping = new HashMap<>();
        mapping.putAll(ConditionBuilder.operators_and_values(operators,query.token_list,query.graph));
        //System.out.println(mapping.toString());
        //System.out.println(query.token_list.toString());

        query.conditions = ConditionBuilder.operator_to_fields(mapping,query);
        ConditionBuilder.value_to_fields(query);
        ConditionBuilder.search_logical_connective(query);
    }

    private static boolean howmany_finder(HashMap<IndexedWord,String> query_words) throws IOException {
        boolean found = false;
        ArrayList<String> query = new ArrayList<>();
        for(Map.Entry<IndexedWord,String> entry : query_words.entrySet())
            query.add(entry.getValue());

        String query_string = query.toString().split("\\[")[1].replaceAll(",","");
        Pattern pattern = Pattern.compile(".*/VB\\b how/WRB\\b many/JJ\\b");
        Matcher matcher = pattern.matcher(query_string);
        if (matcher.find()){
            found = true;
            //System.out.println("Counting query with \"how many\" expression ");
            //System.out.println(matcher.group(0));
        }
        return found;
    }

    public static Set<IndexedWord> related_words(IndexedWord word, SemanticGraph graph, Set<IndexedWord> toremove){
        Set<IndexedWord> setofrelated = new HashSet<>();
        for(IndexedWord related : graph.getChildrenWithRelns(word,Arrays.asList(UniversalEnglishGrammaticalRelations.COMPOUND_MODIFIER, UniversalEnglishGrammaticalRelations.ADJECTIVAL_MODIFIER, UniversalEnglishGrammaticalRelations.getNmod("of")))){
            if(Mapper.is_operator(related.value()))
                continue;
            toremove.add(related);
            if(related.value().compareTo(Main.collection_word)==0 && word.value().compareTo("number")!=0)
                continue;
            setofrelated.addAll(related_words(related,graph, toremove));
        }
        setofrelated.add(word);
        return setofrelated;
    }



    public void query_type(char verb_flag){
        if(verb_flag == 'C'){
            for (Map.Entry<List<IndexedWord>,List<String>> entry : query.searching_fields.entrySet()){
                List<String> add_list = new ArrayList<>();
                List<String> rem_list = new ArrayList<>();
                for(String ixd : entry.getValue())
                    if(!ixd.contains("$")){
                        add_list.add("$count:"+ixd);
                        rem_list.add(ixd);
                    }
                entry.getValue().removeAll(rem_list);
                entry.getValue().addAll(add_list);
            }
        }
        int count_agg=0;
        if(verb_flag == 'R'){
            for (Map.Entry<List<IndexedWord>,List<String>> entry : query.searching_fields.entrySet()){
                for(String ixd : entry.getValue())
                    if(ixd.contains("$") && !(ixd.contains("$year") || ixd.contains("$month") || ixd.contains("$week"))){
                        count_agg++;
                    }
            }
        }
        if(verb_flag=='R' && count_agg==query.searching_fields.size())
            verb_flag='G';
        else if(verb_flag=='R' && count_agg != 0 && count_agg< query.searching_fields.size()){
            verb_flag='M';
        }
        System.out.println(verb_flag + " Searched fields: "  + query.searching_fields.toString());
        System.out.println("Grouping clauses: " + query.grouping_clauses.toString());
        System.out.println("Sorting clauses: " + query.sorting_clauses.toString());
    }

}
