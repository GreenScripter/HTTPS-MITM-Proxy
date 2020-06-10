import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import duoedit.Batch;
import duoedit.BatchResponse;
import duoedit.Challenge;
import duoedit.Lesson;
import duoedit.Pair;

public class Editors {
	
	/**
	 * Prevents the client from uploading cookies to the server.
	 */
	public static ContentEditor noCookies() {
		return new ContentEditor() {
			
			public void editOutgoingHTTP(Request r) {
				for (int i = 0; i < r.inHeaders.size(); i++) {
					if (r.inHeaders.get(i).equals("Cookie")) {
						r.inHeaders.remove(i);
						r.inValues.remove(i);
						i--;
					}
				}
			}
			
			public void editIncomingHTTP(Response r) {}
			
			public boolean outgoing() {
				return true;
			}
			
			public boolean incoming() {
				return false;
			}
		};
	}
	/**
	 * Disables health and ads in duolingo.
	 */
	public static ContentEditor duolingo() {
		return new ContentEditor() {
			
			public void editOutgoingHTTP(Request r) {
				r.disableCompression();
			}
			
			@SuppressWarnings("unchecked")
			public void editIncomingHTTP(Response r) {
				Gson gson = new Gson();
				String s = new String(r.body);
				//disable health
				if (s.contains("\\\"healthEnabled\\\":true,")) {
					s = s.replace("\\\"healthEnabled\\\":true,", "\\\"healthEnabled\\\":false,");
					r.body = s.getBytes();
				}
				//disable ads
				if (s.contains("\\\"adsEnabled\\\":true")) {
					s = s.replace("\\\"adsEnabled\\\":true", "\\\"adsEnabled\\\":false");
					r.body = s.getBytes();
				}//
				r.body = s.getBytes();
				//parse the lesson
				try {
					Lesson l = gson.fromJson(s, Lesson.class);
					if (l.challenges != null && !l.challenges.isEmpty()) {
						for (Challenge c : l.challenges) {
							System.out.println("> " + c.type);
							String type = c.type;
							if (type.equals("translate")) {
								System.out.println(c.prompt);
								System.out.println(c.correctSolutions);
							}
							if (type.equals("match")) {
								for (Pair p : c.pairs) {
									System.out.println(p.fromToken + ", " + p.learningToken);
								}
								
							}
							if (type.equals("judge")) {
								for (int i : c.correctIndices) {
									System.out.println(c.choices.get(i));
								}
								
							}
							if (type.equals("form")) {
								System.out.println(c.promptPieces);
								System.out.println(c.choices.get(c.correctIndex));
								
							}
							if (type.equals("listen")) {
								System.out.println(c.prompt);
								System.out.println(c.solutionTranslation);
								
							}
							if (type.equals("assist")) {
								System.out.println(c.prompt);
								System.out.println(c.choices.get(c.correctIndex));
								
							}
							if (type.equals("select")) {
								System.out.println(c.prompt);
								System.out.println(((Map<String, String>) c.choices.get(c.correctIndex)).get("phrase"));
								
							}
						}
						r.body = s.getBytes();
					}
					
					//Remove extra lessons.
					try {
						//It's not pretty, but the point of using the raw Gson types instead of Objects, is so that small changes to irrelevant parts won't break the parser.
						JsonElement lesson = gson.toJsonTree(gson.fromJson(s, Object.class));
						JsonArray challenges = lesson.getAsJsonObject().get("challenges").getAsJsonArray();
						boolean basics = lesson.getAsJsonObject().get("metadata").getAsJsonObject().get("skill_name").getAsString().equals("BASICS");
						List<JsonObject> objects = new ArrayList<>();
						for (int i = 0; i < challenges.size(); i++) {
							if (challenges.get(i).getAsJsonObject().get("type").getAsString().equals("listen") || challenges.get(i).getAsJsonObject().get("type").getAsString().equals("speak")) {
								System.out.println("Removed Lesson: Listen/Speak");
								
								objects.add(challenges.remove(i).getAsJsonObject());
								i--;
							}
						}
						
						for (int i = 0; i < challenges.size(); i++) {
							if (challenges.get(i).getAsJsonObject().get("type").getAsString().equals("translate")) {
								try {
									JsonElement e = challenges.get(i).getAsJsonObject().get("compactTranslations");
									if (e.isJsonArray()) {
										e.getAsJsonArray().add("skip");
									} else {
										JsonArray array = new JsonArray();
										array.add(e);
										array.add("skip");
										challenges.set(i, array);
									}
								} catch (Exception e) {
								}
								if (challenges.size() > 9 || (challenges.size() > 2 && basics)) {
									System.out.println("Removed Lesson: Too Many");
									challenges.remove(i);
									i--;
								}
							}
						}
						System.out.println("Is basics: " + basics);
						if (basics) {
							for (int i = 0; i < challenges.size(); i++) {
								if (challenges.size() > 2) {
									System.out.println("Removed Lesson: Too Many Basics");
									challenges.remove(i);
									i--;
								}
							}
							for (int i = 0; i < challenges.size(); i++) {
								JsonElement challenge = challenges.get(i);
								try {
									if (challenge.isJsonObject()) {
										System.out.println(challenge.getClass());
										int correctI = challenge.getAsJsonObject().get("correctIndices").getAsJsonArray().get(0).getAsInt();
										JsonElement correct = challenge.getAsJsonObject().get("choices").getAsJsonArray().remove(correctI);
										challenge.getAsJsonObject().get("choices").getAsJsonArray().add(correct);
										challenge.getAsJsonObject().add("correctIndices", new JsonArray());
										challenge.getAsJsonObject().get("correctIndices").getAsJsonArray().add(new JsonPrimitive(2));
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								
							}
						}
						int z = 0;
						for (JsonObject o : objects) {
							z++;
							if (z > 2) {
								System.out.println("Maxed out speaking lessons, removed " + (objects.size() - z + 1) + ".");
								break;
							}
							challenges.add(o);
						}
						objects = new ArrayList<>();
						for (int i = 0; i < challenges.size(); i++) {
							objects.add(challenges.get(i).getAsJsonObject());
						}
						
						Collections.shuffle(objects);
						
						for (int i = 0; i < challenges.size(); i++) {
							challenges.remove(0);
							i--;
						}
						for (JsonObject o : objects) {
							challenges.add(o);
						}
						
						r.body = gson.toJson(lesson).getBytes();
					} catch (Exception e) {
					}
				} catch (Exception e) {
					
				}
			}
			
			public boolean outgoing() {
				return true;
			}
			
			public boolean incoming() {
				return true;
			}
			
		};
	}
	
	/**
	 * Doubles duolingo points, as well as all features of the normal duolingo mod.
	 */
	public static ContentEditor duolingoExtras() {
		ContentEditor duo = duolingo();
		return new ContentEditor() {
			
			public boolean outgoing() {
				return true;
			}
			
			public boolean incoming() {
				return true;
			}
			
			public void editOutgoingHTTP(Request r) {
				duo.editOutgoingHTTP(r);
				String s = new String(r.body);
				//Doubles all the points for the lesson, and is applied after the heart bonus for a maximum number of 38 points per lesson.
				if (s.contains("\"hasBoost\":false")) {
					s = s.replace("\"hasBoost\":false", "\"hasBoost\":true");
					r.body = s.getBytes();
				}
				//Forces the extra points for getting questions right to be always enabled.
				if (s.contains("\"enableBonusPoints\":false")) {
					s = s.replace("\"enableBonusPoints\":false", "\"enableBonusPoints\":true");
					r.body = s.getBytes();
				}
				//Despite hearts no longer being in the app, having 4 of them left still gives you 4 points.
				if (s.contains("\"heartsLeft\":0,")) {
					s = s.replace("\"heartsLeft\":0,", "\"heartsLeft\":4,");
					r.body = s.getBytes();
				}
				try {
					Gson gson = new Gson();
					LessonRequest lr = gson.fromJson(new String(r.body), LessonRequest.class);
					if (lr.skillId.toLowerCase().contains("6a5b92b4287b31db3613f5c74fac02f2")) {
						lr.challengeTypes.clear();
						lr.challengeTypes.add("judge");
						r.body = gson.toJson(lr).getBytes();
					}
					
				} catch (Exception e) {
					
				}
			}
			
			public void editIncomingHTTP(Response r) {
				duo.editIncomingHTTP(r);
				
				//supposed to sort iOS gem chest rewards so that you always get the most value, needs an update.
				Gson gson = new Gson();
				JsonElement el = gson.toJsonTree(gson.fromJson(new String(r.body), Object.class));
				if (el.isJsonObject()) {
					if (el.getAsJsonObject().has("responses")) {
						Batch b = gson.fromJson(new String(r.body), Batch.class);
						for (BatchResponse br : b.responses) {
							try {
								if (gson.toJsonTree(gson.fromJson(br.body, Object.class)).getAsJsonObject().has("practiceReminderSettings")) {
									JsonObject setup = gson.toJsonTree(gson.fromJson(br.body, Object.class)).getAsJsonObject();
									for (Entry<String, JsonElement> e : setup.entrySet()) {
										if (e.getKey().equals("currencyRewardBundles")) {
											JsonElement chestsBundle = e.getValue().getAsJsonArray().get(0);
											JsonArray chests = chestsBundle.getAsJsonObject().get("rewards").getAsJsonArray();
											JsonElement chest1 = chests.remove(0);
											JsonElement chest2 = chests.remove(0);
											JsonElement chest3 = chests.remove(0);
											int amount1 = chest1.getAsJsonObject().get("amount").getAsInt();
											int amount2 = chest2.getAsJsonObject().get("amount").getAsInt();
											int amount3 = chest3.getAsJsonObject().get("amount").getAsInt();
											if (amount1 >= amount2 && amount1 >= amount3) {
												chests.add(chest1);
												if (amount2 >= amount3) {
													chests.add(chest2);
													chests.add(chest3);
												} else {
													chests.add(chest3);
													chests.add(chest2);
												}
											} else if (amount2 >= amount1 && amount2 >= amount3) {
												chests.add(chest2);
												if (amount3 >= amount1) {
													chests.add(chest3);
													chests.add(chest1);
												} else {
													chests.add(chest1);
													chests.add(chest3);
												}
											} else if (amount3 >= amount1 && amount3 >= amount2) {
												chests.add(chest3);
												if (amount2 >= amount1) {
													chests.add(chest2);
													chests.add(chest1);
												} else {
													chests.add(chest1);
													chests.add(chest2);
												}
											}
											System.out.println(e.getValue());
											
										}
									}
									br.body = gson.toJson(setup);
								}
							} catch (Exception e) {
								
							}
						}
						r.body = gson.toJson(b).getBytes();
					}
				}
			}
		};
	}
}
