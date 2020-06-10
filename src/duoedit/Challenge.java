package duoedit;

import java.util.List;

public class Challenge {
	public String sentenceId;
	public String prompt;
	public String sourceLanguage;
	public String tts;
	public List<String> wrongTokens;
	public List<String> compactTranslations;
	public List<Object> progressUpdates;
	public List<Integer> correctIndices;
	public List<Token> tokens;
	
	public Object grader;
	
	public List<String> correctTokens;
	
	public String targetLanguage;
	public String id;
	public List<String> newWords;
	public List<String> correctSolutions;
	public DuoCharacter character;
	public List<Object> choices;
	public List<Pair> pairs;
	public String type;
	public Object metadata;
	public String solutionTranslation;
	public int correctIndex;
	public List<String> promptPieces;

}