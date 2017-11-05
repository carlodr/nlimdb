package com.company;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.WordStemmer;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

public class Query {

    public String qnl_string;
    public Annotation annotation;
    public CoreMap sentence;
    public HashMap<IndexedWord,String> words_to_stem;

    public HashMap<List<IndexedWord>,List<String>> grouping_clauses;
    public HashMap<List<IndexedWord>,List<String>> sorting_clauses;
    public HashMap<List<IndexedWord>,List<String>> searching_fields;
    public HashMap<List<IndexedWord>, List<String>> condition_fields;
    public List<Condition> conditions;
    public Set<IndexedWord> index_for_edge_to_remove;
    public SemanticGraph graph;
    public HashSet<IndexedWord> token_list;
    public HashMap<IndexedWord,SemanticGraphEdge> negation;

    public Query(String qnl, Annotation a){
        annotation = a;
        qnl_string=qnl;
        sentence = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);
        Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
        WordStemmer ls = new WordStemmer();
        ls.visitTree(tree);
        //tree.pennPrint();
        graph = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);
        //System.out.println(graph.toString(SemanticGraph.OutputFormat.LIST));
        words_to_stem=new HashMap<>();
        index_for_edge_to_remove = new HashSet<>();
        grouping_clauses = new HashMap<>();
        sorting_clauses = new HashMap<>();
        searching_fields = new HashMap<>();
        token_list = new HashSet<>();
        condition_fields = new HashMap<>();
        conditions = new ArrayList<>();
    }
}
