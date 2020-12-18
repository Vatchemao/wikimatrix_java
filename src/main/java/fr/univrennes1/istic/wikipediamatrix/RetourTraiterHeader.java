package fr.univrennes1.istic.wikipediamatrix;

import java.util.List;
import java.util.Map;

public class RetourTraiterHeader {

	private List<String> listeHeaders;
	private Map<Integer, Rowspan> mapRowspans;

	public RetourTraiterHeader(List<String> listeHeaders, Map<Integer, Rowspan> mapRowspans) {
		this.listeHeaders = listeHeaders;
		this.mapRowspans = mapRowspans;
	}

	public List<String> getListeHeaders() {
		return listeHeaders;
	}

	public void setListeHeaders(List<String> listeHeaders) {
		this.listeHeaders = listeHeaders;
	}

	public Map<Integer, Rowspan> getMapRowspans() {
		return mapRowspans;
	}

	public void setMapRowspans(Map<Integer, Rowspan> mapRowspans) {
		this.mapRowspans = mapRowspans;
	}

}
