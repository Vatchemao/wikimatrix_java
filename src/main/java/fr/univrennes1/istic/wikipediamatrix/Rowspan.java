package fr.univrennes1.istic.wikipediamatrix;

public class Rowspan {

	private int rowspanResiduel;
	private String texte;

	public Rowspan(int rowspanResiduel, String texte) {
		this.rowspanResiduel = rowspanResiduel;
		this.texte = texte;
	}
	
	public int getRowspanResiduel() {
		return rowspanResiduel;
	}

	public void setRowspanResiduel(int rowspanResiduel) {
		this.rowspanResiduel = rowspanResiduel;
	}

	public String getTexte() {
		return texte;
	}

	public void setTexte(String texte) {
		this.texte = texte;
	}

}
