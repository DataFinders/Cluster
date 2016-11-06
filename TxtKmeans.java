package org.makespace.Kmeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import org.fnlp.app.keyword.AbstractExtractor;
import org.fnlp.app.keyword.WordExtract;
import org.fnlp.nlp.cn.CNFactory;
import org.fnlp.nlp.cn.tag.CWSTagger;
import org.fnlp.nlp.corpus.StopWords;
/*
 * 矩阵类
 */
class Matrix{
	public String[][] fileName;//文件名  + 所属聚类
	public double matrix[][]; //向量矩阵
	public int  dimension ; //矩阵维数
	public int length;      //矩阵行数，即文本个数
	//构造函数
	public Matrix(int dimension ,int length){
		this.dimension = dimension;
		this.length = length;
		this.matrix = new double[length][dimension];
		this.fileName = new String[length][2];
	}
}

public class TxtKmeans {
	public static void main(String[] args) throws Exception{
		
		long startTime = System.currentTimeMillis();
		
		//聚类k的个数
		int k_number = 4;
		
		CNFactory factory = CNFactory.getInstance("models");
		StopWords sw= new StopWords("models/stopwords");
        CWSTagger seg = new CWSTagger("models/seg.m");

		File file = new File("D:\\Reduce\\test");
		File[] files = file.listFiles();
		
		//生成每个文本的关键词集
		String[][] txtWords = getTxtWords(files,factory,sw,seg);
		System.out.println("各文本关键词：");
		for(int i=0;i<files.length;i++)
			System.out.println(txtWords[i][0]+":"+txtWords[i][1]);
		
		//生成关键词集
		String[] keyList = getKeyList(txtWords,files.length);
		System.out.print("文本关键词集合：[");
		for(int i=1;i<keyList.length;i++)
			System.out.print(keyList[i]+" ");
		System.out.println("]");
		
		//生成向量矩阵
		Matrix matrix = getMatrix(txtWords, keyList, files.length, keyList.length);
		System.out.println("向量矩阵：");
		for(int i=0;i<files.length;i++){
			System.out.print(matrix.fileName[i][0]+"-->");
			for(int j=0;j<matrix.dimension;j++)
				System.out.print(matrix.matrix[i][j]+"  ");
			System.out.println("---"+matrix.dimension);
			}
		
		//生成初始质心
		double[][] k = new double[k_number][keyList.length];
		//初始质心为0-100平均分布
		for(int n=0;n<k_number;n++){
			for(int i=(n==0?0:(keyList.length)*n/k_number+1);i<(keyList.length)*(n+1)/k_number;i++){
				k[n][i] = (i<=(keyList.length)/k_number*(n+1)?100:0);
			}
		}
		//初始质心为对角线
		/*for(int n=0;n<k_number;n++){
			for(int i=0;i<keyList.length;i++)
				k[n][i] = 100/(k_number-1)*n;	
		}*/
		System.out.println("初始质心：");
		for(int i=0;i<k_number;i++){
			System.out.print("k"+i+":");
			for(int j=0;j<keyList.length;j++)
				System.out.print(k[i][j]+",");
			System.out.println();
		}
		
		//迭代至新旧质点之间距离小于等于0.1或迭代次数超过100
		for(int i=0;i<100;i++){
			
			System.out.println("第"+(i+1)+"迭代");
			
			//计算所有向量距离质心的距离并进行分类
			iteration(k, matrix,k_number);
			
			
			System.out.println("当前文本聚类分布：");
			for(int j=0;j<files.length;j++)
				System.out.println(matrix.fileName[j][0]+" belong to ：cluster"+matrix.fileName[j][1]);
			//记录当前质心
			double[][] tmp_k = k;
			
			//更新质点
			k = getNewK(matrix,k_number);
			
			for(int i1=0;i1<k_number;i1++){
				System.out.print("k"+i1+":");
				for(int j=0;j<keyList.length;j++)
					System.out.print(k[i1][j]+",");
				System.out.println();
			}
			//若新旧质点间距离小于0.1，停止迭代
			int tmp = 1;
			for(int n=0;n<k_number;n++){
				if(getDistance(tmp_k[n], k[n])>0.1)
					tmp = 0;
			}
			if(tmp == 1)
				break;
			
			
		}
		
		//输出结果
		String[] result = new String[k_number];
		for(int i=0;i<files.length;i++){
			for(int n=0;n<k_number;n++){
				if(matrix.fileName[i][1].equals(""+(n+1))){
					result[n] += matrix.fileName[i][0]+" ";
					result[n] = result[n].replace("null", "");
					break;
				}
			}
		}
		System.out.println("结果：");
		for(int n=0;n<k_number;n++)
			System.out.println("cluster"+(n+1)+"：" + result[n]);
		
		long endTime = System.currentTimeMillis();
		System.out.println("耗时：" + (endTime-startTime)/1000+"s");
	}
	
	
	
	
	
	/*
	 * 查找关键字
	 */
		public static ArrayList<String> GetKeyword(String News,int keywordsNumber,StopWords sw,CWSTagger seg) throws Exception{
	        ArrayList<String> keywords=new ArrayList<String>();
	        AbstractExtractor key = new WordExtract(seg,sw);
	        Map<String,Integer> ans = key.extract(News, keywordsNumber);
	        for (Map.Entry<String, Integer> entry : ans.entrySet()) {
	           String keymap = entry.getKey().toString();
	           String value = entry.getValue().toString();
	           keymap = keymap + "=" + value;
	           keywords.add(keymap);
	        }
	       return keywords;
	    }

	/*
	 * 生成每个文本的关键词集
	 */

	public static String[][] getTxtWords(File[] files,CNFactory factory,StopWords sw,CWSTagger seg) throws Exception {
		String[][] keyList = new String[files.length][2];
		//System.out.println(files.length);
		for(int i=0;i<files.length;i++)
		{
			@SuppressWarnings("resource")
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(files[i]),"UTF-8"));
			String temp=null;
			StringBuffer sb= new StringBuffer();
			temp=br.readLine();
			while(temp!=null)
				{
					sb.append(temp+" ");
					temp=br.readLine();
				}
			String words = factory.tag2String(sb.toString());
			
			//清洗数据
			words = words.replace("，", "");
			words = words.replace("；", "");
			words = words.replace("& ", "");
			words = words.replace("&&&&", "");
			words = words.replace("）", "");
			words = words.replace("％", "");
			words = words.replace("：", "");
			words = words.replace("（", "");
			words = words.replace("？", "");
			words = words.replace("！", "");
			words = words.replace("\r", "");
			words = words.replace("\n", "");
			words = words.replace("\r\n", "");
			words = words.replace("\n\r", "");
			words = words.replace("nbsp", "");
			words = words.replace("&nbsp&nbsp&nbsp&nbsp", "");
			
			String[] tmpWords = words.split(" ");
			words = "";
			for(int k=0;k<tmpWords.length;k++){
				String[] word = tmpWords[k].split("/");
				if(word[1].equals("名词") || word[1].equals("动词"))//只保留名词和动词
				{
					words += word[0] + " ";
				}
			}
			words = clearRepeat(words);
			keyList[i][0] = files[i].getName();
			keyList[i][1] = words;
		}
		return keyList;
	}
	/*
	 * 去除每个文本词集中重复的词，并为每个词按照出现频率赋予权重
	 */
	private static String clearRepeat(String words){
		String[] startWords = words.split(" ");//初始词集，词集中存在大量重复的词
		String[] Words = new String[startWords.length];//用于保存初始词集除去重复后的词集
		int[] value = new int[startWords.length];//用于保存每个词的权重，下标与Words对应
		int j = 0;//记录Words[]数组下标
		for(int i = 0;i<startWords.length;i++){
			int k;//用于接收tmpWords[i]在Words中的对应的下标
			if(startWords[i].equals("") || startWords[i].equals("是")){ //去除值为空或为“是”的情况，FNLP词性标注“是”似乎是名词或者动词，需要在此处去掉
				continue;
			}else if((k=inArray(Words, startWords[i])) != -1){//如果tmpWords[i]在Words中，则该词对应权重加10
				value[k] += 10;
			}else{//如果tmpWords[i]不在Words中，则将该词加入Words数组中，对应的初始权重为10
				value[j] = 10;
				Words[j++] = startWords[i];
			}
		}
		int sortValue[][] = sortArray(value, j);
		String wordList ="";//字符串，保存合并权重后的词集，形式为“word_1=value_1 word_2=value_2 ..... word_j=value_j ”
		for(int i = 0; i<j;i++){
			String tmp = Words[sortValue[i][1]]+"="+sortValue[i][0];
			wordList += tmp+" ";
		}
		return wordList;
	}
	/*
	 * 判断word是否在String数组words中，如果在返回在words数组中的下标，不在则返回-1
	 */
	private static int inArray(String[] words,String word){
		for(int i = 0;i<words.length;i++){
			if(words[i] == null){
				break;
			}else if(words[i].equals(word)){
				return i;
			}
		}
		return -1;
	}
	/*
	 * 将value数组降序处理，返回一个二维数组，第一维：value元素降序后的数组;第二维：value元素对应的原下标
	 */
	private static int[][] sortArray(int[] value,int length){
		int[][] sortValue = new int[length][2];
		for(int i=0;i<length;i++){
			sortValue[i][0] = value[i];
			sortValue[i][1] = i;
		}
		for(int i=0;i<length;i++){
			int k = i;
			for(int j=i+1;j<length;j++){
				if(sortValue[k][0] <= sortValue[j][0])
					k = j;
			}
			int t0 = sortValue[k][0];
			sortValue[k][0] = sortValue[i][0];
			sortValue[i][0] = t0;
			int t1 = sortValue[k][1];
			sortValue[k][1] = sortValue[i][1];
			sortValue[i][1] = t1;
		}
		return sortValue;
	}
	/*
	 * 读取文本集，生成关键词集
	 */
	public static String[] getKeyList(String[][] txtKeys,int filesLength ) throws Exception{
		String keyList = new String();
		for(int i=0;i<filesLength;i++){
			String[] keys = txtKeys[i][1].split(" ");
			for(int k=0;k<keys.length;k++){
				String k_v[] = keys[k].split("=");
				if(keyList.indexOf(k_v[0]) == -1)
					keyList = keyList + " " + k_v[0];
			}
		}
		String[] keylists = keyList.split(" ");
		String[] keyLists = new String[keylists.length-1];
		for(int i=0;i<keylists.length-1;i++)
			keyLists[i] = keylists[i+1];
		return keyLists;
	}
	
	/*
	 * 生成矩阵
	 */
	public static Matrix getMatrix(String[][] txtKeys,String[] keyList,int filesLength,int dimension) {
		Matrix matrix = new Matrix(dimension, filesLength);
		for(int i=0;i<filesLength;i++){
			matrix.fileName[i][0] = txtKeys[i][0]; 
			for(int k=0;k<keyList.length;k++){
				String[] keys = txtKeys[i][1].split(" ");
				double tmp = 0;
				for(int j=0;j<keys.length;j++)
				{
					String[] k_v = keys[j].split("=");
					if(keyList[k].equals(k_v[0]))
						tmp = (double)Integer.parseInt(k_v[1]) ;
				}
				matrix.matrix[i][k]= tmp;
			}
			matrix.fileName[i][1] = "";
		}
		return matrix;
	}
	
	/*
	 * 计算所有向量距离质心的距离，并进行分类
	 */
	public static void iteration(double[][] k,Matrix matrix,int k_number) throws UnsupportedEncodingException{
		for(int i=0;i<matrix.length;i++){
			double[][] cluster = new double[k_number][2];//{{1,0},{2,0},{3,0}};
			for(int n=0;n<k_number;n++){
				cluster[n][0] = (double)(n+1);
				cluster[n][1] = 0;
			}
				
			//计算当前向量距各个质点的距离
			for(int j=0;j<k_number;j++)
				cluster[j][1] = getDistance(matrix.matrix[i], k[j]);
			
			//查询最短距离
			double distance = getDistance(matrix.matrix[i], k[0]);
			for(int j=1;j<k_number;j++){
				distance = Math.min(getDistance(matrix.matrix[i], k[j]),distance);
			}
			
			//分类
			for(int j=0;j<k_number;j++)
				if(cluster[j][1] == distance)
					matrix.fileName[i][1] = ""+(int)cluster[j][0];
		}
	}
	
	/*
	 * 计算两向量之间的距离
	 */
	private static double getDistance(double[]  vector,double[] k) throws UnsupportedEncodingException{
		double sum = 0;
		for(int i=0;i<vector.length;i++){
			sum += Math.pow((k[i]-vector[i]), 2);
		}
		return Math.sqrt(sum);
	}
	
	/*
	 * 更新质点
	 */
	private static double[][] getNewK(Matrix matrix,int k_number) {
		double[][] k = new double[k_number][matrix.dimension];
		for(int i=0;i<matrix.dimension;i++){
			double[] clu_sum = new double[k_number];//{0,0,0}
			int[] clu_num =new int[k_number] ;//{0,0,0};
			for(int j=0;j<matrix.length;j++){
				for(int n=0;n<k_number;n++)
					if(matrix.fileName[j][1].equals(""+(n+1))){
						clu_sum[n] += matrix.matrix[j][i];
						clu_num[n]++;
						break;
					}
			}
			for(int n=0;n<k_number;n++){
				if(clu_num[n] != 0)
					k[n][i] = clu_sum[n]/clu_num[n];
				else{
					k[n][i] = 0; 
					}
			}
		}
		return k;
	}
	
}
