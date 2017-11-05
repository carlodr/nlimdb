package com.company;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations;

import java.util.*;

public class ConditionBuilder {

    public static List<Condition> operator_to_fields(HashMap<IndexedWord,List<IndexedWord>> operators, Query query){
        HashMap<List<IndexedWord>,List<String>> condition_fields = query.condition_fields;
        SemanticGraph graph = query.graph;
        Set<IndexedWord> remained_word = query.token_list;
        HashMap<IndexedWord,String> words_to_stem = query.words_to_stem;
        HashMap<IndexedWord,SemanticGraphEdge> negated = query.negation;

        List<Condition> list_cond = new ArrayList<>();
        List<IndexedWord> be_have = new ArrayList<>();
        for(IndexedWord i : remained_word)
            if(i.value().compareTo("be")==0 || i.value().compareTo("have")==0)
                be_have.add(i);

        another_field:
        for(Map.Entry<List<IndexedWord>,List<String>> entry : condition_fields.entrySet()){
            Set<IndexedWord> ll = new HashSet<>(entry.getKey());
            for(IndexedWord iw : entry.getKey()){
                for(IndexedWord beh : be_have){
                    if (graph.containsEdge(iw,beh) && !graph.reln(iw,beh).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(iw,beh).equals(UniversalEnglishGrammaticalRelations.getConj("or")))
                        ll.add(beh);
                    if (graph.containsEdge(beh,iw) && !graph.reln(beh,iw).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(beh,iw).equals(UniversalEnglishGrammaticalRelations.getConj("or")))
                        ll.add(beh);
                }
            }
            Condition c = new Condition(new ArrayList<>(entry.getKey()),entry.getValue());
            for(IndexedWord condition_word : ll){
                another_operator:
                for(Map.Entry<IndexedWord,List<IndexedWord>> op : operators.entrySet()){
                    List<IndexedWord> temp = new ArrayList<>(op.getValue());
                    temp.add(op.getKey());
                    for(IndexedWord ixw: temp){
                        if (graph.containsEdge(condition_word,ixw) && !graph.reln(condition_word,ixw).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(condition_word,ixw).equals(UniversalEnglishGrammaticalRelations.getConj("or"))) {
                            //System.out.println("Per il campo " + entry.toString() + " condizione: " + op.getValue());
                            c.add_condition(op.getKey(),op.getValue(),words_to_stem, negated, graph);
                            continue another_operator;
                        }
                        if (graph.containsEdge(ixw,condition_word) && !graph.reln(ixw,condition_word).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(ixw,condition_word).equals(UniversalEnglishGrammaticalRelations.getConj("or"))) {
                            //System.out.println("Per il campo " + entry.toString() + " condizione: " + op.getValue());
                            c.add_condition(op.getKey(),op.getValue(),words_to_stem, negated, graph);
                            continue another_operator;
                        }
                    }
                }
            }
            list_cond.add(c);
        }
        //System.out.println(list_cond.toString());
        return list_cond;
    }

    public static HashMap<IndexedWord,List<IndexedWord>> operators_and_values(List<IndexedWord> operators, Set<IndexedWord> remained_words, SemanticGraph graph){
        Set<IndexedWord> indtoremove = new HashSet<>();
        HashMap<IndexedWord,List<IndexedWord>> list = new HashMap<>();
        for(IndexedWord word: operators){
            Set<IndexedWord> rel_words = new HashSet<>();
            List<IndexedWord> ll = new ArrayList<>();
            rel_words.addAll(graph.getParents(word));
            rel_words.addAll(graph.getChildren(word));
            for(IndexedWord w : rel_words)
                for(IndexedWord ww : remained_words){
                    SemanticGraphEdge edg = graph.getEdge(w,word);
                    boolean cond;
                    if(edg!=null)
                        cond = !graph.reln(w,word).equals(UniversalEnglishGrammaticalRelations.getConj("or")) && !graph.reln(w,word).equals(UniversalEnglishGrammaticalRelations.getConj("and"));
                    else
                        cond = !graph.reln(word,w).equals(UniversalEnglishGrammaticalRelations.getConj("or")) && !graph.reln(word,w).equals(UniversalEnglishGrammaticalRelations.getConj("and"));
                    if(w.equals(ww) && w.tag().contains("CD") && cond){
                        ll.add(w);
                        indtoremove.add(w);
                        //System.out.println("trovato valore: " + w + "per l'operatore: " + word);
                        for(IndexedWord ixw : graph.getChildrenWithReln(w,UniversalEnglishGrammaticalRelations.getConj("and")))
                            if(ixw.tag().contains("CD") && word.value().compareTo("between")==0){
                                ll.add(ixw);
                                indtoremove.add(ixw);
                                //System.out.println("trovato valore: " + ixw + "per l'operatore: " + word);
                            }
                    }
                    if(w.equals(ww) && !w.tag().contains("CD") && word.value().compareTo("different")==0 && (graph.reln(word,w)!=null && graph.reln(word,w).equals(UniversalEnglishGrammaticalRelations.getNmod("from")))){
                        ll.addAll(QueryAnalyzer.related_words(w,graph,indtoremove));
                        //ll.add(w);
                        //indtoremove.add(w);
                        indtoremove.add(w);
                    }
                }
            if(ll.size()>0){
                //ll.add(word);
                list.put(word,ll);
            }
        }
        remained_words.removeAll(indtoremove);
        return list;
    }

    public static void value_to_fields(Query query){
        List<Condition> conditions = query.conditions;
        SemanticGraph graph = query.graph;
        Set<IndexedWord> remained_word = query.token_list;
        HashMap<IndexedWord,String> words_to_stem = query.words_to_stem;
        HashMap<IndexedWord,SemanticGraphEdge> negated = query.negation;

        Set<IndexedWord> toremove = new HashSet<>();
        List<IndexedWord> be_have = new ArrayList<>();
        List<IndexedWord> final_values = new ArrayList<>(remained_word);
        for(IndexedWord i : remained_word)
            if(i.value().compareTo("be")==0 || i.value().compareTo("have")==0)
                be_have.add(i);
        final_values.removeAll(be_have);
        for(IndexedWord fv : final_values)
            if(fv.tag().contains("IN"))
                toremove.add(fv);
        final_values.removeAll(toremove);
        IndexedWord uguale = new IndexedWord();
        uguale.setValue("equal");
        uguale.setTag("JJ");
        uguale.setLemma("equal");
        List<IndexedWord> val = new ArrayList<>();
        for(Condition entry : conditions){
            if(!entry.condition.isEmpty())
                continue;
            Set<IndexedWord> ll = new HashSet<>(entry.field);
            for(IndexedWord iw : entry.field){
                for(IndexedWord beh : be_have){
                    if (graph.containsEdge(iw,beh) && !graph.reln(iw,beh).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(iw,beh).equals(UniversalEnglishGrammaticalRelations.getConj("or")))
                        ll.add(beh);
                    if (graph.containsEdge(beh,iw) && !graph.reln(beh,iw).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(beh,iw).equals(UniversalEnglishGrammaticalRelations.getConj("or")))
                        ll.add(beh);
                }
            }
            for(IndexedWord condition_word : ll){
                for(IndexedWord ixw: final_values){
                    List<IndexedWord> temp = new ArrayList<>();
                    if (ixw.tag().charAt(0)!='V' && graph.containsEdge(condition_word,ixw) && !graph.reln(condition_word,ixw).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(condition_word,ixw).equals(UniversalEnglishGrammaticalRelations.getConj("or"))) {
                        for(IndexedWord ww : QueryAnalyzer.related_words(ixw,graph,toremove))
                            if(final_values.contains(ww) && !val.contains(ww)){
                                temp.add(ww);
                                val.add(ww);
                            }
                        if(temp.size()>0){
                            //System.out.println("Per il campo " + entry.field + " condizione: " + temp.toString());
                            entry.add_condition(uguale,temp,words_to_stem, negated, graph);
                            continue;}
                    }
                    if (graph.containsEdge(ixw,condition_word) && !graph.reln(ixw,condition_word).equals(UniversalEnglishGrammaticalRelations.getConj("and")) && !graph.reln(ixw,condition_word).equals(UniversalEnglishGrammaticalRelations.getConj("or"))) {
                        for(IndexedWord ww : QueryAnalyzer.related_words(ixw,graph,toremove))
                            if(final_values.contains(ww) && !val.contains(ww)){
                                temp.add(ww);
                                val.add(ww);
                            }
                        if(temp.size()>0){
                            //System.out.println("Per il campo " + entry.field + " condizione: " + ixw + " " + ixw.index());
                            entry.add_condition(uguale,temp,words_to_stem, negated, graph);}
                    }
                }
            }
        }
        remained_word.removeAll(val);
        //System.out.println(conditions.toString());
    }

    public static void search_logical_connective(Query q){
        List<Condition> conditions = q.conditions;
        SemanticGraph graph = q.graph;
        Set<IndexedWord> temp = new HashSet<>();
        HashMap<HashMap<Set<IndexedWord>,Set<IndexedWord>>,String> listlist = new HashMap<>();
        for(Condition cc : conditions) {
            if (cc.condition.size() == 1)
                cc.logical.put(cc.condition,"-");
            else{
                for(Map.Entry<Set<IndexedWord>,Set<IndexedWord>> entry : cc.condition.entrySet()){
                    temp.addAll(entry.getKey());
                    temp.addAll(entry.getValue());
                }
                temp.addAll(cc.field);
                for(SemanticGraphEdge sg : graph.findAllRelns(UniversalEnglishGrammaticalRelations.getConj("and"))){
                    if(temp.contains(sg.getGovernor()) && temp.contains(sg.getDependent()))
                        cc.logical.put(cc.condition,"and");
                }
                for(SemanticGraphEdge sg : graph.findAllRelns(UniversalEnglishGrammaticalRelations.getConj("or"))){
                    if(temp.contains(sg.getGovernor()) && temp.contains(sg.getDependent()))
                        cc.logical.put(cc.condition,"or");
                }
            }
            System.out.println("for field: " + cc.field + " condition is:" + cc.logical.toString());
        }
    }


}
