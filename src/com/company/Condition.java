package com.company;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations;

import java.util.*;

public class Condition {
    public List<IndexedWord> field;
    public List<String> string_representation;
    public HashMap<Set<IndexedWord>,Set<IndexedWord>> condition = new HashMap<>();
    public HashMap<HashMap<Set<IndexedWord>,Set<IndexedWord>>,String> logical = new HashMap<>();

    public Condition(List<IndexedWord> condition_fields, List<String> representation){
        field=new ArrayList<>();
        field.addAll(condition_fields);
        string_representation = new ArrayList<>();
        string_representation.addAll(representation);
    }

    public void add_condition(IndexedWord operator, List<IndexedWord> value, HashMap<IndexedWord,String> words_to_stem, HashMap<IndexedWord,SemanticGraphEdge> negated, SemanticGraph graph){
        int size = value.size();
        Set<IndexedWord> valori = new LinkedHashSet<>();
        HashMap<Integer,IndexedWord> lop = new HashMap<>();
        for(IndexedWord iw : value)
            lop.put(iw.index(),iw);
        SortedSet<Integer> keys = new TreeSet<>(lop.keySet());
        for (Integer key : keys) {
            valori.add(lop.get(key));
        }

        List<IndexedWord> temp_neg_find = new ArrayList<>(valori);
        temp_neg_find.add(operator);
        temp_neg_find.addAll(this.field);
        IndexedWord negation=null;
        for(IndexedWord tofind : temp_neg_find)
            if(negated.containsKey(tofind))
                negation = negated.get(tofind).getDependent();
        if(negation==null){
            for(IndexedWord tofind : temp_neg_find){
                if(graph.containsVertex(tofind)){
                    Set<IndexedWord> listofparents = graph.getParentsWithReln(tofind, UniversalEnglishGrammaticalRelations.DIRECT_OBJECT);
                    if(listofparents.size()>0)
                        for(IndexedWord iw : listofparents)
                            if(negated.containsKey(iw))
                                negation = negated.get(iw).getDependent();
                }
            }
        }
        if(negation==null)
            condition.put(valori, new HashSet<>(Collections.singletonList(operator)));
        else{
            condition.put(valori, new HashSet<>(Arrays.asList(operator,negation)));
        }

        String string_value="";
        for(IndexedWord ww : valori)
            string_value = string_value + words_to_stem.get(ww).split("/")[0] + " ";
        string_value = string_value.substring(0,string_value.length()-1);
    }

    @Override
    public String toString() {
        return "{" +
                "field=" + string_representation +
                ", condition=" + condition +
                '}';
    }

    public HashMap<HashMap<Set<IndexedWord>,Set<IndexedWord>>,String> getLogical(){
        return this.logical;
    }

}
