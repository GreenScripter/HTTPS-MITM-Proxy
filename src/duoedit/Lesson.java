package duoedit;
import java.util.List;

public class Lesson {
	public String fromLanguage;
	public boolean beginner;
	public String learningLanguage;
	public Object explanations;
	
	public TrackingProperties trackingProperties;
	
	public List<Object> progressUpdates;
	
	public List<Challenge> challenges;
	
	public LessonMetadata metadata;
	public String type;
	public String id;
	public String skillId;
	
}

class TrackingProperties {
	int num_challenges_gt;
	int num_challenges_generated;
	boolean lexemes_were_reordered;
	boolean is_simplified_algorithm;
	int num_challenges_gt_translate;
	int skill_x_coord;
	boolean read_from_cache;
	int distinct_sentences_count;
	int tree_level;
	int max_repeated_sentence_count;
	String from_language;
	String skill_name;
	int max_repeated_challenge_count;
	String skill_id;
	String learning_language;
	int sentences_count;
	int distinct_undirected_sentences_count;
	int num_challenges_gt_listen;
	String data_version;
	int num_challenges_gt_target_learning_judge;
	int num_challenges_gt_tap;
	int max_repeated_undirected_sentence_count;
	int num_challenges_gt_form;
	boolean offline;
	String skill_tree_id;
	String type;
	int num_challenges_gt_reverse_tap;
	Metadata metadata;
}
class Token {
	Object hintTable;
	String tts;
	String value;
}
class DuoCharacter {
	String url;
	String gender;
}
class Choice {
	String tts;
	String text;
}
class Metadata {
	Object tagged_kc_ids;
	Object non_character_tts;
	String sentence;
	String solution_key;
	String source_language;
	String activity_uuid;
	List<String> wrong_tokens;
	List<String> lexeme_ids_to_update;
	List<String> unknown_words;
	String specific_type;
	List<String> wrong_lexemes;
	int num_comments;
	String generator_id;
	String text;
	String type;
	Object new_explanation_ids;
	Object generic_lexeme_map;
	List<String> lexeme_ids;
	List<String> teaches_lexeme_ids;
	List<String> tokens;
	String sentence_id;
	int discussion_count;
	String target_language_name;
	List<String> highlight;
	String translation;
	String from_language;
	boolean has_tts;
	String learning_language;
	boolean has_accents;
	List<String> lexemes_to_update;
	List<String> knowledge_components;
	String target_language;

}
class LessonMetadata {
	String language_string;
	boolean global_practice;
	String hints_url;
	boolean beginner;
	Object mixture_models;
	
	double pass_strength;
	String id;
	String skill_id;
	double min_strength_increment;
	double min_strength_decrement;
	int skill_index;
	String skill_tree_id;
	String type;
	String explanation;
	String skill_title;
	List<String> teaches_lexeme_ids;
	String ui_language;
	Object lexeme_updates;
	String from_language;
	String skill_name;

	int skill_tree_level;
	List<String> target_lexeme_ids;
	String language;
	int kc_strength_model_version;
	boolean tts_enabled;
	String skill_color;
}
