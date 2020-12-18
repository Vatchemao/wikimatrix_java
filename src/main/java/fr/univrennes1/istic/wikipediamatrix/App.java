package fr.univrennes1.istic.wikipediamatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;

public class App {

	private static String BASE_WIKIPEDIA_URL = "https://en.wikipedia.org/wiki/";
	private static String outputDirHtml = "output" + File.separator + "html" + File.separator;
	private static String outputDirWikitext = "output" + File.separator + "wikitext" + File.separator;
	private static File file = new File("inputdata" + File.separator + "wikiurls.txt");
	private static String url;
	private static int nurl = 0;
	private static int nbEchecs = 0;
	private static RetourExtraction retour = null;

	private static final Logger logger = LogManager.getLogger(App.class);

    public static void main( String[] args ) throws IOException {
    	
		logger.debug("Début du main");
		
		List<String> listeEchecs = new ArrayList<String>();
		List<Element> listeTableaux = new ArrayList<Element>();
		
		WikipediaHTMLExtractor extracteur = new WikipediaHTMLExtractor(BASE_WIKIPEDIA_URL, outputDirHtml, outputDirWikitext, file);
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		while ((url = br.readLine()) != null) {
	    	logger.debug("On extrait les tableaux de l'url n° " + String.valueOf(nurl));
	    	retour = extracteur.extraire(url);
	    	if (retour.isEnEchec()) {
	    		nbEchecs += 1;
	    		listeEchecs.add(url);
	    	}
	    	nurl ++;
	    	listeTableaux.addAll(retour.getListeTableaux());
	    }
			
//		url = "Comparison_between_Esperanto_and_Ido";
//		retour = extracteur.extraire(url);
//		listeTableaux.addAll(retour.getListeTableaux());

	    br.close();
        Statistiques statistiques = new Statistiques(listeTableaux);

        logger.info("");
        logger.info(listeTableaux.size() + " tableaux ont été extraits.");
        logger.info("");
        logger.info("Le plus court comporte " + statistiques.getMinLignes() + " ligne(s).");
        logger.info("Le plus long comporte " + statistiques.getMaxLignes() + " lignes.");
        logger.info("Ils comptent en moyenne " + statistiques.getMoyenneLignes() + " lignes, avec un écart-type de " + statistiques.getSdLignes() + ".");
        logger.info("");
        logger.info("Le plus étroit comporte " + statistiques.getMinColonnes() + " colonne(s).");
        logger.info("Le plus large comporte " + statistiques.getMaxColonnes() + " colonnes.");
        logger.info("Ils comptent en moyenne " + statistiques.getMoyenneColonnes() + " colonnes, avec un écart-type de " + statistiques.getSdColonnes() + ".");
        logger.info("");
        logger.info("Le plus petit comporte " + statistiques.getMinCellules() + " cellule(s).");
        logger.info("Le plus grand comporte " + statistiques.getMaxCellules() + " cellules.");
        logger.info("Ils comptent en moyenne " + statistiques.getMoyenneCellules() + " cellules, avec un écart-type de " + statistiques.getSdCellules() + ".");
        logger.info("");
        logger.info("Le nom de colonne le plus fréquent est : " + statistiques.getModeNomDeColonne().get(0) + " (" + statistiques.getModeNomDeColonne().get(1) + " occurrences).");
        logger.info("");        
		logger.info("Il y a " + nbEchecs + " échecs d'intégration. Les voici :");

		for (String echec : listeEchecs) {
			logger.info("    - " + echec);
		}

        logger.info("");
	    logger.debug("FIN DU TRAITEMENT");
    }
}
