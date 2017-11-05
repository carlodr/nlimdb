package com.company;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.stanford.nlp.ling.IndexedWord;

import java.io.IOException;
import java.util.*;

public class Mapper {

    public static void field_mapping(HashMap<String, ArrayList<String>> campi, HashMap<List<IndexedWord>,List<String>> searching) {
        fld: for(Map.Entry<List<IndexedWord>, List<String>> ent : searching.entrySet()){
            List<IndexedWord> s = ent.getKey();
            ArrayList<String> mappings = new ArrayList<>();
            if(s.size()==1){
                lp: for(Map.Entry<String, ArrayList<String>> entry : campi.entrySet()) {
                    ArrayList<String> value = entry.getValue();
                    String temp="";
                    String comp="";
                    ArrayList<String> comparators=new ArrayList<>();
                    switch(s.get(0).toString().split("/")[1].charAt(0)){
                        case 'N':
                            temp="/N";
                            break;
                        case 'V':
                            temp="/V";
                            break;
                        default:
                            continue fld;
                    }
                    for (String ss : value)
                        if(ss.contains(temp)){
                            comp = ss.split("/")[0];
                            break;
                        }
                    if(comp.contains(" "))
                        Collections.addAll(comparators,comp.split(" "));
                    else
                        comparators.add(comp);
                    for(String cmp : comparators){
                        if(cmp.compareTo(s.get(0).toString().split("/")[0])==0){
                            //System.out.println(cmp + " ugualeuguale" + entry.getKey());
                            mappings.add(entry.getKey());
                            //System.out.println("per il campo cercato: " + s + " sono stati trovati i seguenti mapping:");
                            //System.out.println(mappings.toString());
                            continue lp;
                        }
                    }
                    for(String cmp : comparators){
                        ILexicalDatabase db = new NictWordNet();
                        //WS4JConfiguration.getInstance().setMFS(true);
                        double sim = similarity(db,cmp,s.get(0).toString().split("/")[0],temp.charAt(1));
                        //System.out.println("comparing " + cmp + " ... " + s.split("/")[0]);
                        if(sim > 0.95 & sim <=1.0){
                            //System.out.println(sim + "   "+ s.split("/")[0] + "  " + cmp + " :.:.:." + entry.getKey());
                            mappings.add(entry.getKey());
                        }
                    }
                }
            }
            else{
                loop: for(IndexedWord str : s){
                    lp: for(Map.Entry<String, ArrayList<String>> entry : campi.entrySet()) {
                        ArrayList<String> value = entry.getValue();
                        String temp="";
                        String comp="";
                        ArrayList<String> comparators=new ArrayList<>();
                        switch(str.toString().split("/")[1].charAt(0)){
                            case 'N':
                                temp="/N";
                                break;
                            case 'V':
                                temp="/V";
                                break;
                            default:
                                continue loop;
                        }
                        for (String ss : value)
                            if(ss.contains(temp)){
                                comp = ss.split("/")[0];
                                break;
                            }
                        if(comp.contains(" "))
                            Collections.addAll(comparators,comp.split(" "));
                        else
                            comparators.add(comp);
                        for(String cmp : comparators){
                            if(cmp.compareTo(str.toString().split("/")[0])==0){
                                //System.out.println(cmp + " ugualeuguale" + entry.getKey());
                                if(!mappings.contains(entry.getKey()))
                                    mappings.add(entry.getKey());
                                else{
                                    mappings.clear();
                                    mappings.add(entry.getKey());
                                    break loop;
                                }
                                //System.out.println("per il campo cercato: " + s + " sono stati trovati i seguenti mapping:");
                                //System.out.println(mappings.toString());
                                continue lp;
                            }
                        }
                        for(String cmp : comparators){
                            ILexicalDatabase db = new NictWordNet();
                            //WS4JConfiguration.getInstance().setMFS(true);
                            double sim = similarity(db,cmp,str.toString().split("/")[0],temp.charAt(1));
                            //System.out.println("comparing " + cmp + " ... " + s.split("/")[0]);
                            if(sim > 0.95 & sim <=1.0){
                                //System.out.println(sim + "   "+ s.split("/")[0] + "  " + cmp + " :.:.:." + entry.getKey());
                                if(!mappings.contains(entry.getKey()))
                                    mappings.add(entry.getKey());
                                else{
                                    mappings.clear();
                                    mappings.add(entry.getKey());
                                    break loop;
                                }
                            }
                        }
                    }
                }
            }
            //System.out.println("per il campo cercato: " + ent.getKey() + " sono stati trovati i seguenti mapping:");
            //System.out.println(mappings.toString());
            ArrayList<String> toadd = new ArrayList<>();
            ArrayList<String> toremove = new ArrayList<>();
            if(mappings.size()>0)
                for(IndexedWord ixd : s){
                    if(ixd.value().compareTo("year")==0){
                        for(String maps : mappings){
                            if(maps.contains("date") || maps.contains("Date")){
                                toadd.add("$year:"+maps);
                                toremove.add(maps);
                            }
                        }
                    }
                    if(ixd.value().compareTo("month")==0){
                        for(String maps : mappings){
                            if(maps.contains("date") || maps.contains("Date")){
                                toadd.add("$month:"+maps);
                                toremove.add(maps);
                            }
                        }
                    }
                    if(ixd.value().compareTo("week")==0){
                        for(String maps : mappings){
                            if(maps.contains("date") || maps.contains("Date")){
                                toadd.add("$week:"+maps);
                                toremove.add(maps);
                            }
                        }
                    }
                }
            mappings.removeAll(toremove);
            mappings.addAll(toadd);
            //System.out.println(mappings.toString());
            ent.getValue().addAll(mappings);
            if(mappings.size()==0){
                IndexedWord data = new IndexedWord();
                data.setValue("date");
                data.setTag("NN");
                data.setLemma("date");
                if(s.get(0).toString().split("/")[0].compareTo("year")==0 && mappings.size()==0){
                    HashMap<List<IndexedWord>,List<String>> r = new HashMap<>();
                    r.put(new ArrayList<>(Collections.singletonList(data)),mappings);
                    field_mapping(campi,r);
                    ArrayList<String> ez = new ArrayList<>();
                    for(String se : mappings){
                        se = "$year:"+se;
                        ez.add(se);
                    }
                    ent.getValue().addAll(ez);
                }
                if(s.get(0).toString().split("/")[0].compareTo("month")==0 && mappings.size()==0){
                    HashMap<List<IndexedWord>,List<String>> r = new HashMap<>();
                    r.put(new ArrayList<>(Collections.singletonList(data)),mappings);
                    //r.put("date/NN",mappings);
                    field_mapping(campi,r);
                    ArrayList<String> ez = new ArrayList<>();
                    for(String se : mappings){
                        se = "$month:"+se;
                        ez.add(se);
                    }
                    ent.getValue().addAll(ez);
                }
                if(s.get(0).toString().split("/")[0].compareTo("week")==0 && mappings.size()==0){
                    HashMap<List<IndexedWord>,List<String>> r = new HashMap<>();
                    r.put(new ArrayList<>(Collections.singletonList(data)),mappings);
                    //r.put("date/NN",mappings);
                    field_mapping(campi,r);
                    ArrayList<String> ez = new ArrayList<>();
                    for(String se : mappings){
                        se = "$day:"+se;
                        ez.add(se);
                    }
                    ent.getValue().addAll(ez);
                }
            }
            //System.out.println(ent.getValue().toString());
        }
    }

    public static ArrayList<String> aggregator_mapping(HashMap<String, ArrayList<String>> campi, HashMap<List<IndexedWord>,List<String>> searched) throws IOException {
        HashMap<String,String[]> aggregator_map = new HashMap<>();
        aggregator_map.put("maximum",new String[]{"maximum","maximal","top","biggest","greatest","largest"});
        aggregator_map.put("minimum",new String[]{"minimum","minimal","littlest","smallest","least","lowest"});
        aggregator_map.put("sum",new String[]{"sum","total","amount"});
        aggregator_map.put("average",new String[]{"average","medium","intermediate","median","middle","mean"});
        aggregator_map.put("first",new String[]{"first","early","ahead"});
        aggregator_map.put("last",new String[]{"last","ending","end","latest","finishing"});
        aggregator_map.put("count",new String[]{"number"});

        ArrayList<String> aggregation_token = new ArrayList<>();
        loop: for(Map.Entry<List<IndexedWord>, List<String>> field_entry : searched.entrySet()){
            boolean contenuto = false;
            List<IndexedWord> field_expr = field_entry.getKey();
            for(IndexedWord s : field_expr)
                for(Map.Entry<String, String[]> entry : aggregator_map.entrySet()) {
                    String[] agg_values = entry.getValue();
                    for (String value : agg_values){
                        if(s.toString().split("/")[1].startsWith("J") || s.toString().split("/")[1].startsWith("N")){
                            if(value.compareTo(s.toString().split("/")[0])==0){
                                //System.out.print(field + " ha possibile operatore aggregato " + s + " equivalente a " + entry.getKey() + " del campo ");
                                String operatore = entry.getKey();
                                String camp="";
                                cp: for(String st : field_entry.getValue())
                                    for (String stt : campi.get(st))
                                        if(stt.contains("/N")){
                                            String comp = stt.split("/")[0];
                                            for(String vl : agg_values)
                                                if(comp.contains(vl)){
                                                    contenuto = true;
                                                    camp = stt.split("/")[0];
                                                    break cp;
                                                }
                                        }
                                if(!contenuto){
                                    //System.out.println(field_expr + " ha sicuro operatore aggregato " + s.toString() + " equivalente a " + operatore + " del campo/i " + field_entry.getValue().toString());
                                    ArrayList<String> poppi = new ArrayList<>();
                                    for(String mps : searched.get(field_expr))
                                        poppi.add("$"+ operatore +":" + mps);
                                    searched.get(field_expr).clear();
                                    searched.put(field_expr,poppi);
                                    continue loop;
                                }
                                else{
                                    //System.out.println(field_expr + " ha possibile operatore aggregato " + s.toString() + " equivalente a " + operatore + " del campo " + camp);
                                    ArrayList<String> poppi = new ArrayList<>();
                                    for(String mps : searched.get(field_expr))
                                        poppi.add("$"+ operatore +":" + mps);
                                    searched.get(field_expr).addAll(poppi);
                                    continue loop;
                                }
                            }
                        }
                    }
                }
        }
        return aggregation_token;
    }

    public static double similarity(ILexicalDatabase db, String w1, String w2, char pos){
        WS4JConfiguration.getInstance().setMFS(true);
        RelatednessCalculator wup = new WuPalmer(db);
        double maxScore = -1D;
        String ps="";
        switch(pos){
            case 'V':
                ps="v";
                break;
            case 'N':
                ps="n";
                break;
        }
        List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(w1, ps);
        List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(w2, ps);
        //String ab="";
        for(Concept synset1: synsets1) {
            for (Concept synset2: synsets2) {
                Relatedness relatedness = wup.calcRelatednessOfSynset(synset1, synset2);
                double score = relatedness.getScore();
                if (score > maxScore) {
                    //ab = synset1.toString() + synset2.toString();
                    maxScore = score;
                }
            }
        }
        if (maxScore == -1D) {
            maxScore = 0.0;
        }
        //if(ab.compareTo("")!=0)
        //   System.out.println(maxScore + "   csaca  " + ab);
        return maxScore;
    }

    public static boolean is_operator(String word){
        HashMap<String,String[]> aggregator_map = new HashMap<>();
        aggregator_map.put("greater",new String[]{"greater","more","larger","higher","bigger","after","least"});
        aggregator_map.put("less",new String[]{"less","lesser","lower","minor","before","most"});
        aggregator_map.put("equal",new String[]{"equal","equivalent","correspondent"});
        aggregator_map.put("between",new String[]{"between","among"});
        aggregator_map.put("not_equal",new String[]{"different","divergent"});
        for(Map.Entry<String, String[]> field_entry : aggregator_map.entrySet())
            for(String ss : field_entry.getValue())
                if(ss.compareTo(word)==0)
                    return true;
        return false;
    }

    public static boolean is_aggregator(String word){
        HashMap<String,String[]> aggregator_map = new HashMap<>();
        aggregator_map.put("maximum",new String[]{"maximum","maximal","top","biggest","greatest","largest"});
        aggregator_map.put("minimum",new String[]{"minimum","minimal","littlest","smallest","lowest"});
        aggregator_map.put("sum",new String[]{"sum","total","amount"});
        aggregator_map.put("average",new String[]{"average","medium","intermediate","median","middle","mean"});
        aggregator_map.put("first",new String[]{"first","early","ahead"});
        aggregator_map.put("last",new String[]{"last","ending","end","latest","finishing"});
        aggregator_map.put("count",new String[]{"number"});

        for(Map.Entry<String, String[]> field_entry : aggregator_map.entrySet())
            for(String ss : field_entry.getValue())
                if(ss.compareTo(word)==0){
                    return true;
                }
        return false;
    }

}
