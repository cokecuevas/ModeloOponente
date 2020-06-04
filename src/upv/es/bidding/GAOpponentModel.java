package upv.es.bidding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.utility.AdditiveUtilitySpace;
import negotiator.boaframework.opponentmodel.tools.UtilitySpaceAdapter;
import genius.core.issue.Value;

public class GAOpponentModel extends OpponentModel {

	protected SortedOutcomeSpace sortedOutcomeSpace;
	
	protected List<Bid> mLastOpponentOffers;
	protected List<Bid> mBestOwnOffers;
	
	protected List<Bid> poolOffers;
	protected int Z=1;
	protected int N_BEST_OFFERS = 20;
	protected int N_BEST_OPPONENT_OFFERS = 20;
	protected HashMap<String,Integer> OFFERS_RANK_LAPTOP = new HashMap();
	protected HashMap<String,Integer> OFFERS_RANK_HARDDISK = new HashMap();
	protected HashMap<String,Integer> OFFERS_RANK_MONITOR = new HashMap();
	protected double w1 = 0.33;
	protected double w2 = 0.33;
	protected double w3 = 0.33;
	protected double r =  0.03;
	protected double K = 0.001;
	protected boolean start = false;
	protected double timeCoef = 0.05;
	protected double modelOponent = 1;
	
	protected Random randGenerator;
	
	protected void determineBestBids(int n) {
		Iterator<BidDetails> it = sortedOutcomeSpace.getAllOutcomes().iterator();
		while(mBestOwnOffers.size()<n && it.hasNext()) {
			BidDetails bd = it.next();
			mBestOwnOffers.add(bd.getBid());
		}
		
	}
		
	@Override
	public String getName() {
		return "Tiempo pesos";
	}
	
	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		super.init(negotiationSession, parameters);
		
		
		OFFERS_RANK_LAPTOP.put("Dell", 0);
		OFFERS_RANK_LAPTOP.put("Macintosh", 0);
		OFFERS_RANK_LAPTOP.put("HP", 0);
		
		OFFERS_RANK_HARDDISK.put("60 Gb", 0);
		OFFERS_RANK_HARDDISK.put("80 Gb", 0);
		OFFERS_RANK_HARDDISK.put("120 Gb", 0);
		
		OFFERS_RANK_MONITOR.put("19'' LCD", 0);
		OFFERS_RANK_MONITOR.put("20'' LCD", 0);
		OFFERS_RANK_MONITOR.put("23'' LCD", 0);
		
		/**
		 * Sets of relevant offers
		 */
		mLastOpponentOffers = new ArrayList<Bid>();
		mBestOwnOffers = new ArrayList<Bid>();
		poolOffers = new ArrayList<Bid>();
		sortedOutcomeSpace = new SortedOutcomeSpace( this.negotiationSession.getUtilitySpace() );
		
		randGenerator = new Random();
		
		if(parameters.containsKey("n")) {
			N_BEST_OFFERS = parameters.get("n").intValue();
		}
		if(parameters.containsKey("n_opponent")) {
			N_BEST_OPPONENT_OFFERS = parameters.get("n_opponent").intValue();
		}
		
		determineBestBids(N_BEST_OFFERS);
		
	}
	
	@Override
	protected void updateModel(Bid bid, double T) {
	/*	if(mLastOpponentOffers.size()>=N_BEST_OPPONENT_OFFERS) {
			mLastOpponentOffers.remove(0);
		} 
		mLastOpponentOffers.add(bid);
		*/
		
		//w1*(1+p/1+p) + w2*(1/1+p) + w3*(1/1+p)
		//OFFERS_RANK.put()
		if (start) {
		System.out.println("---------X--------------");
		System.out.println("Oferta Número: "+Z);
		Z++;
		HashMap<Integer,Value> values = bid.getValues();
		//
		
		//guardo el mapa anterior
		HashMap<String,Integer> OFFERS_RANK_LAPTOP2 = OFFERS_RANK_LAPTOP;
		HashMap<String,Integer> OFFERS_RANK_HARDDISK2 = OFFERS_RANK_HARDDISK;
		HashMap<String,Integer> OFFERS_RANK_MONITOR2 = OFFERS_RANK_MONITOR;
		
	
		//actualizo el mapa
		for(int i=1; i<4;i++) {
			try {
			OFFERS_RANK_LAPTOP.put(values.get(i).toString(),OFFERS_RANK_LAPTOP.get(values.get(i).toString())+1);
			}catch(Exception e) {
			}
			try {
				OFFERS_RANK_MONITOR.put(values.get(i).toString(),OFFERS_RANK_MONITOR.get(values.get(i).toString())+1);
				}catch(Exception e) {
				}
			try {
				OFFERS_RANK_HARDDISK.put(values.get(i).toString(),OFFERS_RANK_HARDDISK.get(values.get(i).toString())+1);
			}catch(Exception e) {	
			}
		}
		//comparo los mapas para ver si hay cambios y detecto que atributo cambió
		boolean laptop = OFFERS_RANK_LAPTOP.equals(OFFERS_RANK_LAPTOP2);
		boolean harddisk = OFFERS_RANK_HARDDISK.equals(OFFERS_RANK_HARDDISK2);
		boolean monitor = OFFERS_RANK_MONITOR.equals(OFFERS_RANK_MONITOR2);
		//actualizo los pesos según el atributo
		if(laptop) {
			updatePesos("laptop");
		}else if(harddisk) {
			updatePesos("harddisk");
		}else if(monitor) {
			updatePesos("monitor");
		}
		
		//construyo la ecuación con los pesos (clásica) y le sumo el tiempo
		//entre más pasa el tiempo, menos gana la oferta (0.3 para que no sobrepase 1).
		int totalOffer = OFFERS_RANK_LAPTOP.size();
		modelOponent = w1*(OFFERS_RANK_LAPTOP.size()/totalOffer) + 
				w2*(OFFERS_RANK_HARDDISK.size()/totalOffer) + 
				w3*(OFFERS_RANK_MONITOR.size()/totalOffer) - 
			    ((T/1+K*T)-T);
		System.out.println(modelOponent);
		}
		start = true;
		
	}
	
	private void updatePesos(String component) {
		// TODO Auto-generated method stub
		if(component.contains("laptop")) {
			w1 = (w1+r)/(1+r);
			w2 = (w2)/(1+r);
			w3 = (w3)/(1+r);
		}
		else if(component.contains("harddisk")) {
			w1 = (w1)/(1+r);
			w2 = (w2+r)/(1+r);
			w3 = (w3)/(1+r);
		}else if(component.contains("monitor")) {
			w1 = (w1)/(1+r);
			w2 = (w2)/(1+r);
			w3 = (w3+r)/(1+r);
	 }
	}



	@Override
	public double getBidEvaluation(Bid bid) {
		System.out.println("Bid being evaluated");
		if(poolOffers.contains(bid)) {
			return modelOponent;
		}
		
		return 0;
	}
	
	@Override
	public AdditiveUtilitySpace getOpponentUtilitySpace() {	
		AdditiveUtilitySpace utilitySpace = new UtilitySpaceAdapter(this, this.negotiationSession.getDomain());
		return utilitySpace;
	}
	
	@Override
	public Set<BOAparameter> getParameterSpec(){
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("n", 20.0, "The number of own best offers to be used for genetic operations"));
		set.add(new BOAparameter("n_opponent", 20.0, "The number of opponent's best offers to be used for genetic operations"));
		return set;
	}

	

}
