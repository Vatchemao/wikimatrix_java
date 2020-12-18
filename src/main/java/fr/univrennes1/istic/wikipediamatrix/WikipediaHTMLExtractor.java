package fr.univrennes1.istic.wikipediamatrix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import au.com.bytecode.opencsv.CSVWriter;
import uk.gov.nationalarchives.csv.validator.metadata.Row;

public class WikipediaHTMLExtractor {

	private String BASE_WIKIPEDIA_URL;
	private String outputDirHtml;
	private String outputDirWikitext;
	private File file;
	
	private CSVWriter csvWriter;

	private static final Logger logger = LogManager.getLogger(WikipediaHTMLExtractor.class);

	public WikipediaHTMLExtractor(String BASE_WIKIPEDIA_URL, String outputDirHtml, String outputDirWikitext, File file) {
		this.BASE_WIKIPEDIA_URL = BASE_WIKIPEDIA_URL;
		this.outputDirHtml = outputDirHtml;
		this.outputDirWikitext = outputDirWikitext;
		this.file = file;
	}

	/** Méthode permettant d'extraire le contenu de toutes les wikitables présentes sur l'url paramétrique.
	 * Renvoie un objet RetourExtraction, qui contient la liste des tableaux (utile pour les tests unitaires)
	 * et si la lecture est un échec (utile pour les statistiques de fin de traitement). **/
	public RetourExtraction extraire(String url) throws IOException {
	   List<Element> listeRetour = new ArrayList<Element>();
	   boolean enEchec = false;
	   logger.debug("Début de l'extraction");
	   try {
		   String wurl = BASE_WIKIPEDIA_URL + url;
		   logger.debug("On extrait à présent cette page : " + wurl);
		   Document doc = Jsoup.connect(wurl).get();

	       // On itère sur chaque tableau de la page.
	       List<Element> tables = doc.select("table.wikitable");
	       logger.debug("Il y a " + String.valueOf(tables.size()) + " tableaux dans cette page.");
	       for (int i = 0; i < tables.size(); i++ ) {
	    	   Element table = tables.get(i);
	    	   traiterTableau(table, url, i);
	    	   listeRetour.add(table);
	       }
	   } catch(Exception e) {
		   logger.warn("Cette url n'est pas accessible. Le traitement va l'ignorer et se poursuivre.");
		   enEchec = true;
	   }
	   logger.debug("Fin de l'extraction");
       return new RetourExtraction(listeRetour, enEchec);
	}

	/** Méthode permettant de traiter un tableau, en trois temps : d'abord en initialisation le fichier
	 * csv, ensuite écrivant les headers, et enfin en écrivant les lignes. **/
	public void traiterTableau(Element table, String url, int i) throws IOException {
		logger.debug("Début du traitement du tableau n° " + String.valueOf(i + 1));
		try {
			// On initialise le fichier csv.
			String csvFileName = mkCSVFileName(url, i + 1);
			File csvFile = new File(outputDirHtml + csvFileName);
			logger.debug("Le fichier est créé ici : " + outputDirHtml + csvFileName);
			FileWriter fileWriter = new FileWriter(csvFile, StandardCharsets.UTF_8);
			csvWriter = new CSVWriter(fileWriter, ';');

			// On initialise la map de rowspans et les lignes
			Map<Integer, Rowspan> rowspans = new HashMap<Integer, Rowspan>();
			Elements lignes = table.select("tr");
			int largeurTableau = getLargeurTotaleTableau(table);
			logger.debug("Il y a " + String.valueOf(lignes.size()) + " lignes dans ce tableau et sa largeur est de " + largeurTableau + " colonnes.");

			// On traite les headers
			int nombreHeaders = traiterHeaders(lignes, largeurTableau);

			// On crée les lignes
			for (int compteur = nombreHeaders; compteur < lignes.size(); compteur ++) {
				Element ligne = lignes.get(compteur);
				logger.debug("on traite la ligne : " + compteur);
				rowspans = traiterLigne(rowspans, ligne, largeurTableau);
			}
	        csvWriter.close();
		} catch(Exception e) {
			logger.warn("Une erreur est survenue durant le traitement de ce tableau.");
		}
		logger.debug("Fin du traitement du tableau n° " + String.valueOf(i + 1) + ".");
	}

	/** Méthode permettant de traiter les headers, et renvoyant le nombre de headers traités. **/
	public int traiterHeaders(Elements lignes, int largeurTableau) {
		List<Element> headers = new ArrayList<Element>();
		int nombreHeaders = 0; 
		while (isHeader(lignes.get(nombreHeaders))) {
			headers.add(lignes.get(nombreHeaders));
			nombreHeaders ++;
		}
		if (nombreHeaders > 0) {
			ecrireHeaders(headers, largeurTableau);
		}
		return nombreHeaders;
	}
	
	/** Cette méthode permet d'écrire la ou les lignes de headers. S'il n'y a une ou deux lignes de headers,
	 * on n'écrit qu'une seule ligne dans le csv, car même lorsqu'il y en a deux dans la wikitable, le 
	 * véritable en-tête résulte d'une fusion des deux lignes d'en-têtes de la wikitable. S'il y a plus de 
	 * deux headers, pour l'instant, on traite les headers comme des lignes (TODO : traiter plus proprement
	 * ces cas dans une version ultérieure). **/
	public void ecrireHeaders(List<Element> headers, int largeurTableau) {
		switch (headers.size()) {
			case 1:
				logger.debug("On traite un en-tête");
				List<String> listeHeaders1 = traiterUnHeader(headers).getListeHeaders();
				csvWriter.writeNext(transformerListeEnTableau(listeHeaders1));
				break;
			case 2:
				logger.debug("On traite deux en-têtes");
				List<String> listeHeaders2 = traiterDeuxHeaders(headers);
				csvWriter.writeNext(transformerListeEnTableau(listeHeaders2));
				break;
			default:
				Map<Integer, Rowspan> mapRowspans = new HashMap<Integer, Rowspan>();
				for (int i = 0; i < headers.size(); i++) {
					logger.debug("On traite l'en-tête n°" + i);
					Element ligne = headers.get(i);
					mapRowspans = traiterLigne(mapRowspans, ligne, largeurTableau);
				}
				break;
		}
	}

	/** Méthode permettant d'écrire les en-têtes lorsqu'il n'y avait qu'une seule ligne d'en-tête 
	 * dans la wikitable. Renvoie un objet RetourTraiterUnSeulHeader, constitué d'une liste contenant
	 * les en-têtes sous forme de String, et d'une liste des positions des éventuels rowspans
	 * (méthode conçue pour traiter rowspans et colspans). **/
	public RetourTraiterHeader traiterUnHeader(List<Element> headers) {
		// On initialise les résultats.
		Elements cellules = headers.get(0).children();
		List<String> listeHeaders = new ArrayList<String>();
		Map<Integer, Rowspan> mapRowspans = new HashMap<Integer, Rowspan>();
		int decalageColspans = 0;
		for (int j = 0; j < cellules.size(); j++) {
			Element cellule = cellules.get(j);
			if (!cellule.hasAttr("colspan")) {
				if (!cellule.hasAttr("rowspan")) {
					// Cas où il n'y a ni colspan ni rowspan : miam miam, du pain béni !
					listeHeaders.add(cellule.text());
				} else {
					// Cas où il n'y a pas de colspan, mais un rowspan.
					mapRowspans.put(j + decalageColspans, new Rowspan(Integer.valueOf(cellule.attr("rowspan")), cellule.text()));
					listeHeaders.add(cellule.text());
				}
			} else {
				if (!cellule.hasAttr("rowspan")) {
					// Cas où il y a un colspan, mais pas de rowspan.
					for (int k = 0; k < Integer.valueOf(cellule.attr("colspan")); k++) {
						listeHeaders.add(cellule.text());
					}
					decalageColspans += Integer.valueOf(cellule.attr("colspan")) - 1;
				} else {
					// Cas où il y a un colspan et un rowspan.
					for (int l = 0; l < Integer.valueOf(cellule.attr("colspan")); l++) {
						listeHeaders.add(cellule.text());
						mapRowspans.put(j + l + decalageColspans, new Rowspan(Integer.valueOf(cellule.attr("rowspan")), cellule.text()));
					}
					decalageColspans += Integer.valueOf(cellule.attr("colspan")) - 1;
				}
			}
		}
		return new RetourTraiterHeader(listeHeaders, mapRowspans);
	}
	
	/** Méthode permettant de traiter les en-têtes des wikitables présentant des en-têtes sur deux
	 * lignes, avec la première contenant éventuellement des rowspans et des colspans. Renvoie une 
	 * seule liste d'en-têtes sous forme de String, car lorsqu'il y a deux lignes d'en-têtes dans une
	 * wikitable, la véritable en-tête d'une colonne est constituée par la concaténation des deux
	 * en-têtes. **/
	public List<String> traiterDeuxHeaders(List<Element> headers) {
		// On récupère les informations issues du traitement de la première ligne.
		RetourTraiterHeader retourTraiterUnSeulHeader = traiterUnHeader(headers);
		List<String> listeHeaders1 = retourTraiterUnSeulHeader.getListeHeaders();
		Map<Integer, Rowspan> mapRowspans = retourTraiterUnSeulHeader.getMapRowspans();
		// On initialise le résultat.
		List<String> listeHeaders2 = new ArrayList<String>();
		Map<Integer, Rowspan> nouvelleMapRowspans = new HashMap<Integer, Rowspan>();
		// On crée un indice secondaire qui parcourra la liste des cellules de la seconde ligne d'en-tête
		// en même temps que l'indice principal, mais avec un décalage correspondant au nombre de rowspans
		// traités jusque là.
		int idecale = 0;
		Elements cellules2 = headers.get(1).children();
		for (int i = 0; i < listeHeaders1.size(); i++) {
			if (mapRowspans.keySet().contains(i)) {
				listeHeaders2.add(listeHeaders1.get(i));
				idecale -= 1;
			} else {
				listeHeaders2.add(listeHeaders1.get(i) + " " + cellules2.get(idecale).text());
			}
			idecale += 1;
		}
		return listeHeaders2;
	}

	/** Cette méthode permet de traiter une ligne. Un soin particulier a été apporté à la gestion des rowspans.
	 * Cette méthode prend en paramètre une map <Integer, Rowspan>, et retourne une map du même type, ce qui
	 * lui permet de traiter les rowspans que lui ont légués les lignes précédentes, et d'envoyer aux lignes 
	 * cette map, modifiée et potentiellement complétées des nouveaux rowspans dont elle fait l'objet. **/
	public Map<Integer, Rowspan> traiterLigne(Map<Integer, Rowspan> mapRowspans, Element ligne, int largeurTableau) {
		Elements cellulesLigneCourante = ligne.children();
		Map<Integer, Rowspan> retour = new HashMap<Integer, Rowspan>();
		List<String> listeCellulesAEcrire = new ArrayList<String>();
		Rowspan rowspan = null;
		int compteurAnciensRowspansTraites = 0;
		int decalageColspans = 0;
		// Pour compléter la liste nouvellement créée avec les éventuels rowspans résiduels légués
		// par les lignes précédentes, on parcourt la map rowspans. Lorsqu'on trouve un Rowspan à la 
		// position Integer, on insère une nouvelle cellule dans la liste des cellules nouvellement 
		// créée, à la position Integer.
		for (int i = 0; i < largeurTableau; i++) {
			if ((i + decalageColspans) >= largeurTableau) {
				csvWriter.writeNext(transformerListeEnTableau(listeCellulesAEcrire));
				return mapRowspans;
			}
			// On traite d'abord les cas où la cellule est sous le coup d'un rowspan actif : dans
			// un premier temps, on écrit la cellule du rowspan, puis, dans un second temps, celle que  
			// l'on aurait dû écrire s'il n'y avait pas eu de rowspan actif. Enfin, on met à jour
			// le compteur compteurAnciensRowspansTraites, et les objets rowspans utilisés, en décrémentant
			// leur attribut rowspanResiduel (et les supprimant si cet attribut devient nul).
			if (mapRowspans.containsKey(i)) {
				rowspan = mapRowspans.get(i);
				listeCellulesAEcrire.add(rowspan.getTexte());
				// On met à jour le rowspan, et, s'il y a lieu, on l'insère dans la map de résultats.
				rowspan.setRowspanResiduel(rowspan.getRowspanResiduel() - 1);
				if (rowspan.getRowspanResiduel() > 0) {
					retour.put(i, rowspan);
				}
				compteurAnciensRowspansTraites += 1;
			} else {
				// Petit test pour traiter le cas où une cellule manquerait dans une ligne. Si si, ça arrive :'(
				if (cellulesLigneCourante.size() > (i - compteurAnciensRowspansTraites)) {
					Element cellule = cellulesLigneCourante.get(i - compteurAnciensRowspansTraites);
					// On est ici dans le cas où il n'y a pas de rowspan actif pour la cellule en cours. Il faut
					// distinguer le cas dans lequel on est : ni colspan ni rowspan, pas colspan mais rowspan, 
					// colspan mais pas rowspan, colspan et rowspan.
					if (!cellule.hasAttr("colspan")) {
						if (!cellule.hasAttr("rowspan")) {
							// Cas où il n'y a ni colspan ni rowspan : miam miam, du pain béni !
							listeCellulesAEcrire.add(cellule.text());
						} else {
							// Cas où il n'y a pas de colspan, mais un rowspan.
							mapRowspans.put(i + compteurAnciensRowspansTraites, new Rowspan(Integer.valueOf(cellule.attr("rowspan")), cellule.text()));
							listeCellulesAEcrire.add(cellule.text());
						}
					} else {
						if (!cellule.hasAttr("rowspan")) {
							// Cas où il y a un colspan, mais pas de rowspan.
							for (int j = 0; j < Integer.valueOf(cellule.attr("colspan")); j++) {
								listeCellulesAEcrire.add(cellule.text());
							}
							decalageColspans += Integer.valueOf(cellule.attr("colspan")) - 1;
						} else {
							// Cas où il y a un colspan et un rowspan.
							for (int k = 0; k < Integer.valueOf(cellule.attr("colspan")); k++) {
								listeCellulesAEcrire.add(cellule.text());
								mapRowspans.put(i + k + decalageColspans, new Rowspan(Integer.valueOf(cellule.attr("rowspan")), cellule.text()));
							}
							decalageColspans += Integer.valueOf(cellule.attr("colspan")) - 1;
						}
					}
				} else {
					listeCellulesAEcrire.add("");
				}	
			}
		}
		csvWriter.writeNext(transformerListeEnTableau(listeCellulesAEcrire));
		return mapRowspans;
	}


	// ***************************************** METHODES TECHNIQUES *******************************************


	/** Méthode renvoyant true si la ligne en question est une ligne d'en-têtes (c-à-d exclusivement composée
	 * d'éléments th), false sinon. **/
	public boolean isHeader(Element ligne) {
		for (Element element : ligne.children()) {
			if (!element.is("th")) {
				return false;
			}
		}
		return true;
	}

	/** Méthode renvoyant le nom du fichier csv à créer à partir de l'url de la page d'origine
	 * et du rang du tableau dans celle-ci. **/
	public String mkCSVFileName(String url, int n) {
		return url.trim() + "-" + n + ".csv";
	}

	/** Méthode technique permettant de transformer une liste de String en tableaux de String
	 * (utile notamment car le csvWriter prend des tableaux en paramètres).
	 * **/
	public String[] transformerListeEnTableau(List<String> listeHeaders) {
		String[] resultat = new String[listeHeaders.size()];
		for (int i = 0; i < listeHeaders.size(); i++) {
			resultat[i] = listeHeaders.get(i);
		}
		return resultat;
	}
	
	public int getLargeurTotaleTableau(Element tableau) {
		int resultat = 0;
		for (int i = 0; i < tableau.selectFirst("tr").childrenSize(); i++) {
			if (tableau.selectFirst("tr").child(i).hasAttr("colspan")) {
				resultat += Integer.valueOf(tableau.selectFirst("tr").child(i).attr("colspan"));
			} else {
				resultat += 1;
			}
		}
		return resultat;
	}


	// ********************************************* ACCESSEURS ***********************************************


	public String getBASE_WIKIPEDIA_URL() {
		return BASE_WIKIPEDIA_URL;
	}

	public void setBASE_WIKIPEDIA_URL(String bASE_WIKIPEDIA_URL) {
		BASE_WIKIPEDIA_URL = bASE_WIKIPEDIA_URL;
	}

	public String getOutputDirHtml() {
		return outputDirHtml;
	}

	public void setOutputDirHtml(String outputDirHtml) {
		this.outputDirHtml = outputDirHtml;
	}

	public String getOutputDirWikitext() {
		return outputDirWikitext;
	}

	public void setOutputDirWikitext(String outputDirWikitext) {
		this.outputDirWikitext = outputDirWikitext;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

}