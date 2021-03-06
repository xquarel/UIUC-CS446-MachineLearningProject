package com.cs446.ml;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

public class HillaryExplorer {
	private static final String sqlitePath = "/Users/xin/Documents/Develop/CS446/HillaryEmailsAnalyst/data/database.sqlite";
	private static final String stopWordPath = "/Users/xin/Documents/Develop/CS446/HillaryEmailsAnalyst/nlp/stopwords.txt";
	private static final String featurePath = "/Users/xin/Desktop/features_r1.arff";
//	private static final String sql = "SELECT *  FROM Emails where MetadataFrom='H' and MetadataTo in ('abedinh@state.gov', 'millscd@state.gov','sullivanjj@state.gov', 'JilotyLC@state.gov')";

	private Set<String> stopWords;
	private TokenizerFactory<CoreLabel> tokenizerFactory;
	private HashMap<String,Integer> bodyDict;
	private HashMap<String,Integer> subjectDict;
	private List<String> bodyFeatures;
	private List<String> subjectFeatures;
	private Map<String,String> labels;
//	private String classVal;
	private String featuresVal;
	private Map<String,Integer> wc;
	
	private Connection c;
	private Statement stmt;
	private int doc_count;

	public HillaryExplorer() throws Exception{
		stopWords = new HashSet<String>();
		bodyDict = new HashMap<String,Integer>();
		subjectDict = new HashMap<String,Integer>();
		bodyFeatures = new ArrayList<String>();
		subjectFeatures = new ArrayList<String>();
		wc = new HashMap<String, Integer>();
		doc_count=0;
		
		getStopWords();

		c = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		c.setAutoCommit(false);
		System.out.println("Opened database successfully");
		stmt = c.createStatement();

	    tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		
		labels = new HashMap<String,String>();
		labels.put("abedinh@state.gov", "0");//
		labels.put("millscd@state.gov", "0");//
		labels.put("sullivanjj@state.gov", "2");
		labels.put("sulllivanjj@state.gov", "2");
		labels.put("JilotyLC@state.gov", "3");
		labels.put("ValmoroLJ@state.gov", "0");
		labels.put("preines", "0");
		labels.put("sbwhoeop", "6");
		labels.put("cheryl.mills", "0");//
		labels.put("Abedin, Huma", "0");//
		labels.put("hanleymr@state.gov", "0");
		labels.put("vermarr@state.gov", "2");
		labels.put("AbedinH@state.gov", "0");//
		labels.put("slaughtera@state.gov", "0");
//		labels.put("Russorv@state.gov", "3");

		featuresVal = "0,2,3,6";
		

		
		getDictionary();
		
	}

	private void closeConnection() throws Exception {
		stmt.close();
		c.close();
	}
	
	private String getSQL() throws Exception {
		String sql = "SELECT *  FROM Emails where MetadataFrom='H' and MetadataTo in (";
		boolean isFirst = true;
		for (Iterator iterator = labels.keySet().iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			if(isFirst){
				isFirst=false;
			} else {
				sql+=",";
			}

			sql+="'"+name+"'";
		}
		sql+=")";
		return sql;
	}

	private void getStopWords() throws Exception {
		String sCurrentLine;
		FileReader fr=new FileReader(stopWordPath);
		BufferedReader br= new BufferedReader(fr);
		while ((sCurrentLine = br.readLine()) != null){
			stopWords.add(sCurrentLine);
		}
	}

	private void getDictionary() throws Exception {
		Set<String> bodySet = new HashSet<String>();
		Set<String> subjectSet = new HashSet<String>();

		Map<String,Integer> wc_sub = new HashMap<String, Integer>();
		
		ResultSet rs = stmt.executeQuery(getSQL());
		while ( rs.next() ) {
			String to = rs.getString("MetadataTo");
			String emailBody = rs.getString("ExtractedBodyText");
			String emailSubject = rs.getString("ExtractedSubject");
		    List<CoreLabel> tokens = tokenizerFactory.getTokenizer(new StringReader(emailBody)).tokenize();
		    if(tokens.size()>0){
		    	doc_count++;
		    	Set<CoreLabel> tokenSet = new HashSet<CoreLabel>(tokens);
			    for (Iterator iterator = tokenSet.iterator(); iterator.hasNext();) {
					CoreLabel coreLabel = (CoreLabel) iterator.next();
					String label = coreLabel.toString().trim().toLowerCase();
					if(label.length()>1&&!isNumeric(label)&&!isTime(label)&&!stopWords.contains(label.toLowerCase())){
						label=label.replaceAll("[-+^,'\";!?{}]","");
						if(wc.get(label)==null){
							wc.put(label, 1);
						} else {
							Integer c = wc.get(label)+1;
							wc.put(label, c);
						}
					}
				}

			    if(emailSubject.trim().length()>0){
				    List<CoreLabel> subTokens = tokenizerFactory.getTokenizer(new StringReader(emailSubject)).tokenize();
				    Set<CoreLabel> subTokensSet = new HashSet<CoreLabel>(subTokens);

				    for (Iterator iterator = subTokensSet.iterator(); iterator.hasNext();) {
						CoreLabel coreLabel = (CoreLabel) iterator.next();
						String label = coreLabel.toString().trim().toLowerCase();
						if(label.length()>1&&!isNumeric(label)&&!isTime(label)&&!stopWords.contains(label.toLowerCase())){
							label=label.replaceAll("[-+^,'\";!?{}]","");
							if(wc_sub.get(label)==null){
								wc_sub.put(label, 1);
							} else {
								Integer c = wc_sub.get(label)+1;
								wc_sub.put(label, c);
							}
						}
					}
				    for (Iterator iterator = wc_sub.keySet().iterator(); iterator.hasNext();) {
						String key = (String) iterator.next();
						Integer ct = wc_sub.get(key);
						if(ct>=3){
							subjectSet.add(key);
						}
					}

			    }
			    for (Iterator iterator = wc.keySet().iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					Integer ct = wc.get(key);
					if(ct>=3){
						bodySet.add(key);
					}
				}
		    }
		    
		}
		rs.close();

		int idx = 0;
		for(String w:bodySet){
			if(w.trim().length()>0&&!w.equals("''")){
				bodyDict.put(w, idx);
				idx++;
				bodyFeatures.add(w);

			}
		}

		for(String w:subjectSet){
			if(w.trim().length()>0&&!w.equals("''")){
				subjectDict.put(w, idx);
				idx++;
				subjectFeatures.add(w);
			}

		}
	}

	public void writeFeatures(boolean idf) throws Exception {

		PrintWriter featureWriter = new PrintWriter(new FileOutputStream(featurePath, false));		

		featureWriter.println("@relation Email");
		featureWriter.println();

		for (Iterator iterator = bodyFeatures.iterator(); iterator.hasNext();) {
			String f = (String) iterator.next();
			featureWriter.println("@attribute emailBody="+ f + " numeric");
		}

		for (Iterator iterator = subjectFeatures.iterator(); iterator.hasNext();) {
			String f = (String) iterator.next();
			featureWriter.println("@attribute emailSubject="+ f + " numeric");
		}
		

		for (int i = 0; i < 7; i++) {
			featureWriter.println("@attribute day="+ i + " {0,1}");
		}
//		featureWriter.println("@attribute weekday {0,1}");

		for (int i = 0; i < 6; i++) {
			featureWriter.println("@attribute timeframe="+ i + " {0,1}");
		}
//		featureWriter.println("@attribute emailTimeCount numeric");
//		featureWriter.println("@attribute subjectTimeCount numeric");

		
		featureWriter.println("@attribute Class {"+featuresVal+"}");
		featureWriter.println();
		featureWriter.println("@data");

		ResultSet rs = stmt.executeQuery(getSQL());
		while ( rs.next() ) {
			String emailBody = rs.getString("ExtractedBodyText");
			String emailSubject = rs.getString("ExtractedSubject");
			String dateSent = rs.getString("ExtractedDateSent");
			String to = rs.getString("MetadataTo");
		    
		    if(emailBody.trim().length()>0){
			    List<CoreLabel> tokens = tokenizerFactory.getTokenizer(new StringReader(emailBody)).tokenize();

		    	double [] wordCount = new double[bodyDict.size()+subjectDict.size()];
				Arrays.fill(wordCount, 0);

				int emailTimeCount = 0;
				int subTimeCount = 0;
				
				Map<String,Integer> tokenMap = new HashMap<String,Integer>();
				
				for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
					CoreLabel coreLabel = (CoreLabel) iterator.next();
					String w = coreLabel.toString().trim().toLowerCase();
					if(!stopWords.contains(w.toLowerCase())){
						if(isTime(w)){
							emailTimeCount++;
						} else {
							w=w.replaceAll("[-+^,'\";!?{}]","");
							if(idf){
								if(tokenMap.get(w)==null){
									tokenMap.put(w, 0);
								} else {
									int c = tokenMap.get(w)+1;
									tokenMap.put(w, c);
								}
							} else {
								Integer idx = bodyDict.get(w);
								if(idx!=null){
									wordCount[idx]++;
								}
							}
						}
					}
				}
				
				if(idf){
					for (Iterator iterator = tokenMap.keySet().iterator(); iterator.hasNext();) {
						String k = (String) iterator.next();
						Integer idx = bodyDict.get(k);
						if(idx!=null){
							int c = tokenMap.get(k);
							double idfVal = c*Math.log((1.0*doc_count)/wc.get(k));
							wordCount[idx]=idfVal;
						}
	
					}
				}
				
			    List<CoreLabel> subTokens = tokenizerFactory.getTokenizer(new StringReader(emailSubject)).tokenize();
				for (Iterator iterator = subTokens.iterator(); iterator.hasNext();) {
					CoreLabel coreLabel = (CoreLabel) iterator.next();
					String w = coreLabel.toString().trim().toLowerCase();
					if(isTime(w)){
						subTimeCount++;
					} else {
						if(!stopWords.contains(w.toLowerCase())){
							w=w.replaceAll("[-+^,'\";!?{}]","");
							Integer idx = subjectDict.get(w);
							if(idx!=null){
								wordCount[idx]++;
							}
						}
					}

				}
				
				for (int i = 0; i < wordCount.length; i++) {
					featureWriter.print(wordCount[i]);
					featureWriter.print(",");
				}

				
				int [] timeArr = new int[6];
				Arrays.fill(timeArr, 0);
				int [] dayArr = new int[7];
				Arrays.fill(dayArr, 0);

				int isWeekday = 1;
				
				//Saturday, May 30 2009 11:59 PM
				//Sun Oct 25 11:13:172009
				//Monday, October 26, 2009 7:25 AM
				if(dateSent!=null&&dateSent.length()>0){
					if(dateSent.contains(",")){
						String[] parts = dateSent.split(",");
						for (int i = 0; i < parts.length; i++) {
							String d = parts[i].trim().toUpperCase();
							if(d.startsWith("SUN")){
								dayArr[0]=1;
								isWeekday=0;
							} else if(d.startsWith("MON")){
								dayArr[1]=1;
							} else if(d.startsWith("TUE")){
								dayArr[2]=1;
							} else if(d.startsWith("WED")){
								dayArr[3]=1;
							} else if(d.startsWith("TH")){
								dayArr[4]=1;
							} else if(d.startsWith("FRI")){
								dayArr[5]=1;
							} else if(d.startsWith("SAT")){
								dayArr[6]=1;
								isWeekday=0;
							} 
						}
						String[] timeParts = parts[parts.length-1].split(" ");
						if(timeParts.length>=2){
							for (int i = 0; i < timeParts.length; i++) {
								String t = timeParts[i];
								if(t.contains(":")){
									try {
										int hr = Integer.valueOf(t.split(":")[0].trim());
										int idx = hr/3;
										String apm = timeParts[i+1];
										if(apm.trim().equalsIgnoreCase("PM")){
											idx+=3;
										}
										timeArr[idx]=1;
									} catch (Exception e) {
									}

								}
							}
						}
					}

				}
				for (int i = 0; i < dayArr.length; i++) {
					featureWriter.print(dayArr[i]);
					featureWriter.print(",");
				}

				for (int i = 0; i < timeArr.length; i++) {
					featureWriter.print(timeArr[i]);
					featureWriter.print(",");
				} 

				// label
				featureWriter.print(labels.get(to));
				featureWriter.print("\n");
		    }

			

		}
		rs.close();
		featureWriter.close();

	}

	/*
	public void writeFeatures2() throws Exception {

		PrintWriter featureWriter = new PrintWriter(new FileOutputStream(featurePath, false));		

		featureWriter.println("@relation Email");
		featureWriter.println();

		for (Iterator iterator = bodyFeatures.iterator(); iterator.hasNext();) {
			String f = (String) iterator.next();
			featureWriter.println("@attribute emailBody="+ f + " numeric");
		}

		for (Iterator iterator = subjectFeatures.iterator(); iterator.hasNext();) {
			String f = (String) iterator.next();
			featureWriter.println("@attribute emailSubject="+ f + " numeric");
		}

		for (int i = 0; i < 7; i++) {
			featureWriter.println("@attribute day="+ i + " {0,1}");
		}
		for (int i = 0; i < 6; i++) {
			featureWriter.println("@attribute timeframe="+ i + " {0,1}");
		}
		String classVar = "{";
		for (Iterator iterator = labelSet.iterator(); iterator.hasNext();) {
			String c = (String) iterator.next();
			classVar+=c+",";
		}
		classVar=classVar.substring(0, classVar.length()-1);
		classVar+="}";

		featureWriter.println("@attribute Class {0,1,2}");
		featureWriter.println();
		featureWriter.println("@data");

		Set<String> receivers = new HashSet<String>();
		receivers.add("abedin, huma");
		receivers.add("abedinh@state.gov");
		receivers.add("jilotylc@state.gov");
		receivers.add("valmorolj@state.gov");

		ResultSet rs = stmt.executeQuery(sql);
		while ( rs.next() ) {
			String to = rs.getString("MetadataTo");
			String from = rs.getString("MetadataFrom");
			if(from.equals("H")&&receivers.contains(to.toLowerCase())){
				String emailBody = rs.getString("ExtractedBodyText");
				String emailSubject = rs.getString("ExtractedSubject");
				String dateSent = rs.getString("ExtractedDateSent");
				String tokens[] = tokenizer.tokenize(emailBody);
				String subTokens[] = tokenizer.tokenize(emailSubject);

				int [] wordCount = new int[bodyDict.size()+subjectDict.size()];
				Arrays.fill(wordCount, 0);

				for (String w : tokens){
					Integer idx = bodyDict.get(w);
					if(idx!=null){
						wordCount[idx]++;
					}
				}
				for (String w : subTokens){
					Integer idx = subjectDict.get(w);
					if(idx!=null){
						wordCount[idx]++;
					}
				}
				for (int i = 0; i < wordCount.length; i++) {
					featureWriter.print(wordCount[i]);
					featureWriter.print(",");
				}

				int [] timeArr = new int[6];
				Arrays.fill(timeArr, 0);
				int [] dayArr = new int[7];
				Arrays.fill(dayArr, 0);

				//Saturday, May 30 2009 11:59 PM
				//Sun Oct 25 11:13:172009
				//Monday, October 26, 2009 7:25 AM
				if(dateSent!=null&&dateSent.length()>0){
					if(dateSent.contains(",")){
						String[] parts = dateSent.split(",");
						for (int i = 0; i < parts.length; i++) {
							String d = parts[i].trim().toUpperCase();
							if(d.startsWith("SUN")){
								dayArr[0]=1;
							} else if(d.startsWith("MON")){
								dayArr[1]=1;
							} else if(d.startsWith("TUE")){
								dayArr[2]=1;
							} else if(d.startsWith("WED")){
								dayArr[3]=1;
							} else if(d.startsWith("TH")){
								dayArr[4]=1;
							} else if(d.startsWith("FRI")){
								dayArr[5]=1;
							} else if(d.startsWith("SAT")){
								dayArr[6]=1;
							} 
						}
						String[] timeParts = parts[parts.length-1].split(" ");
						if(timeParts.length>=2){
							for (int i = 0; i < timeParts.length; i++) {
								String t = timeParts[i];
								if(t.contains(":")){
									try {
										int hr = Integer.valueOf(t.split(":")[0].trim());
										int idx = hr/3;
										String apm = timeParts[i+1];
										if(apm.trim().equalsIgnoreCase("PM")){
											idx+=3;
										}
										timeArr[idx]=1;
									} catch (Exception e) {
									}

								}
							}
						}
					}

				}
				for (int i = 0; i < dayArr.length; i++) {
					featureWriter.print(dayArr[i]);
					featureWriter.print(",");
				}
				for (int i = 0; i < timeArr.length; i++) {
					featureWriter.print(timeArr[i]);
					featureWriter.print(",");
				}
				receivers.add("abedin, huma");
				receivers.add("abedinh@state.gov");
				receivers.add("jilotylc@state.gov");
				receivers.add("valmorolj@state.gov");

				// label
				int c = 0;
				if(to.equalsIgnoreCase("abedin, huma")||to.equalsIgnoreCase("abedinh@state.gov")){
					c=0;
				} else if(to.equalsIgnoreCase("jilotylc@state.gov")){
					c=1;
				} else if(to.equalsIgnoreCase("valmorolj@state.gov")){
					c=2;
				}
				featureWriter.print(c);
				featureWriter.print("\n");

			}
			



		}
		rs.close();
		featureWriter.close();

	}
	

	private boolean isNotLetter(String c) {
		if(c.length()>1) return false;
		Pattern p = Pattern.compile("[^a-z]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(c);
		boolean b = m.find();
		return b;
	}

	private boolean isSpecialChar(String c) {
		if(c.length()>1) return false;
		Pattern p = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(c);
		boolean b = m.find();
		return b;
	}
	*/
	
	public boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+\\.*\\d*");  //match a number with optional '-' and decimal.
	}
	public boolean isTime(String str)
	{
	  return str.matches("\\d*:\\d+");  //match a number with optional '-' and decimal.
	}

	public static void main( String args[] ) {
		try {
			HillaryExplorer explorer = new HillaryExplorer();
			explorer.writeFeatures(false);
			explorer.closeConnection();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		System.out.println("Operation done successfully");
	}
}
