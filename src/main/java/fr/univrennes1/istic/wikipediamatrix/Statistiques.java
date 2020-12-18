package fr.univrennes1.istic.wikipediamatrix;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Statistiques {

	private List<Element> listeTableaux;

	public Statistiques(List<Element> listeTableaux) {
		this.listeTableaux = listeTableaux;
	}

	// ********************************** Statistiques sur les lignes **********************************
	
	public int getMinLignes() {
		// On initialise à une valeur quelconque, mais pas nulle, pour ne pas rester bloqués à 0.
		int resultat = 100;
		for (Element element : this.listeTableaux) {
			if (element.select("tr").size() < resultat) {
				resultat = element.select("tr").size();
			}
		}
		return resultat;
	}

	public int getMaxLignes() {
		int resultat = 0;
		for (Element element : this.listeTableaux) {
			if (element.select("tr").size() > resultat) {
				resultat = element.select("tr").size();
			}
		}
		return resultat;
	}

	public double getMoyenneLignes() {
		int nombreLignes = 0;
		for (Element element : this.listeTableaux) {
			nombreLignes += element.select("tr").size();
		}
		return nombreLignes/listeTableaux.size();
	}

	public double getSdLignes() {
		List<Integer> listeTailles = new ArrayList<Integer>();
		double sd = 0.00;
		for (Element element : this.listeTableaux) {
			listeTailles.add(element.select("tr").size());
		}
        double moyenne = this.getMoyenneLignes();
        for(int nombre : listeTailles) {
            sd += Math.pow(nombre - moyenne, 2);
        }
        return Math.sqrt(sd/listeTailles.size());
	}


	// ********************************* Statistiques sur les colonnes *********************************


	public int getNombreCellulesDansLigne(Map<Integer, Rowspan> mapRowspans, Element ligne) {
		int resultat = 0;
		if (mapRowspans != null) {
			resultat = mapRowspans.size();			
		}
		for (int i = 0; i < ligne.children().size(); i++) {
			if (ligne.children().get(i).hasAttr("colspan")) {
				resultat += Integer.valueOf(ligne.children().get(i).attr("colspan"));
			} else {
				resultat += 1;
			}
		}
		return resultat;
	}

	public int getMinColonnes() {
		// On initialise à une valeur quelconque, mais pas nulle, pour ne pas rester bloqués à 0.
		int resultat = 100;
		for (Element element : this.listeTableaux) {
			int nombreColonnes = this.getNombreCellulesDansLigne(null, element.selectFirst("tr"));
			if (nombreColonnes < resultat) {
				resultat = nombreColonnes;
			}
		}
		return resultat;
	}

	public int getMaxColonnes() {
		int resultat = 0;
		for (Element element : this.listeTableaux) {
			int nombreColonnes = this.getNombreCellulesDansLigne(null, element.selectFirst("tr"));
			if (nombreColonnes > resultat) {
				resultat = nombreColonnes;
			}
		}
		return resultat;
	}

	public double getMoyenneColonnes() {
		int nombreColonnes = 0;
		for (Element element : this.listeTableaux) {
			nombreColonnes += this.getNombreCellulesDansLigne(null, element.selectFirst("tr"));
		}
		return nombreColonnes/listeTableaux.size();
	}

	public double getSdColonnes() {
		List<Integer> listeTailles = new ArrayList<Integer>();
		double sd = 0.00;
		for (Element element : this.listeTableaux) {
			listeTailles.add(this.getNombreCellulesDansLigne(null, element.selectFirst("tr")));
		}
        double moyenne = this.getMoyenneColonnes();
        for(int nombre : listeTailles) {
            sd += Math.pow(nombre - moyenne, 2);
        }
        return Math.sqrt(sd/listeTailles.size());
	}


	// ********************************* Statistiques sur les cellules *********************************


	public int getNombreCellules(Element element) {
		return element.select("td").size() + element.select("th").size();
	}

	public int getMinCellules() {
		// On initialise à une valeur quelconque, mais pas nulle, pour ne pas rester bloqués à 0.
		int resultat = 100;
		for (Element element : this.listeTableaux) {
			int nombreCellules = this.getNombreCellules(element);
			if (nombreCellules < resultat) {
				resultat = nombreCellules;
			}
		}
		return resultat;
	}

	public int getMaxCellules() {
		int resultat = 0;
		for (Element element : this.listeTableaux) {
			int nombreCellules = this.getNombreCellules(element);
			if (nombreCellules > resultat) {
				resultat = nombreCellules;
			}
		}
		return resultat;
	}

	public double getMoyenneCellules() {
		int nombreColonnes = 0;
		for (Element element : this.listeTableaux) {
			nombreColonnes += this.getNombreCellules(element);
		}
		return nombreColonnes/listeTableaux.size();
	}

	public double getSdCellules() {
		List<Integer> listeTailles = new ArrayList<Integer>();
		double sd = 0.00;
		for (Element element : this.listeTableaux) {
			listeTailles.add(this.getNombreCellules(element));
		}
        double moyenne = this.getMoyenneCellules();
        for(int nombre : listeTailles) {
            sd += Math.pow(nombre - moyenne, 2);
        }
        return Math.sqrt(sd/listeTailles.size());
	}


	// ******************************** Nom de colonne le plus fréquent ********************************
	
	public List<String> getModeNomDeColonne() {
		List<String> resultat = new ArrayList<String>();
		List<String> nomsDeColonnes = new ArrayList<String>();
		for (Element element : this.listeTableaux) {
			nomsDeColonnes.addAll(element.selectFirst("tr").children().eachText());
		}
		String mode = "";
		int frequence = 0;
		int compteur;
		for (int i = 0; i < nomsDeColonnes.size(); i++){
		    compteur = 0 ;
		    for (int j = 0 ;j < nomsDeColonnes.size(); j++){
		         if (nomsDeColonnes.get(i).equals(nomsDeColonnes.get(j))) {
		            compteur += 1;
		        }
		    }
		    if (compteur > frequence) {
		        frequence = compteur;
		        mode = nomsDeColonnes.get(i);
		    }
		}
		resultat.add(mode);
		resultat.add(Integer.toString(frequence));
		return resultat;
	}


	// ******************************************* Accesseurs ******************************************


	public List<Element> getListeTableaux() {
		return listeTableaux;
	}

	public void setListeTableaux(List<Element> listeTableaux) {
		this.listeTableaux = listeTableaux;
	}

}
