package fr.univrennes1.istic.wikipediamatrix;

import java.util.List;

import org.jsoup.nodes.Element;

public class RetourExtraction {

	private List<Element> listeTableaux;
	private boolean enEchec;

	public RetourExtraction(List<Element> listeTableaux, boolean enEchec) {
		this.listeTableaux = listeTableaux;
		this.setEnEchec(enEchec);
	}

	public List<Element> getListeTableaux() {
		return listeTableaux;
	}

	public void setListeTableaux(List<Element> listeTableaux) {
		this.listeTableaux = listeTableaux;
	}

	public boolean isEnEchec() {
		return enEchec;
	}

	public void setEnEchec(boolean enEchec) {
		this.enEchec = enEchec;
	}

}
