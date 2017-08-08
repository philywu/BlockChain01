import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class TxOperation {

	public static String C_PREHASH = "preHash";
	public static String C_PREINDEX = "preIndex";
	public static String C_PREVALUE = "preValue";
	private Map<String, KeyPair> users;
	//private TxHandler txHdl = new TxHandler(new UTXOPool());
	private ArrayList<Transaction> txList = new ArrayList<Transaction>() ;
	

	public TxOperation(Map<String, KeyPair> users) {
		this();
		this.users = users;

	}

	public TxOperation() {

	}

	public Transaction createCoins() {
		Transaction tx = new Transaction();

		tx.addOutput(100, users.get("Luke").getPublic());
		tx.addOutput(200, users.get("Phily").getPublic());
		tx.addOutput(50, users.get("Robert").getPublic());

		// Transaction[] txs = {tx};
		//txHdl.completeTransaction(tx);
		
		addToTxList(tx);
		return tx;
	}
	public Transaction getTransaction(int pos) {
		return txList.get(pos);
	}
	public Transaction getLastTransaction() {
		if (txList.size()>0)
			return txList.get(txList.size()-1);
		else return null;
	}
	public Transaction findTransaction(byte[] hashCode) {
		if(txList.size()>0){
			int index = txList.size() - 1 ; 
			while (index >=0){
				Transaction tx = getTransaction(index);
				//if (tx.getHash().hashCode() == hashCode.hashCode()) {
				if (tx.matchHash(hashCode)) {
					return tx; 
				}
				index --;
			}
			return null; 
		}else {
			return null ; 
		}
	}
	public List<Map> findInput(double targetValue,PublicKey payerAddress ){
		List<Map> inputList = new ArrayList<Map>();
		double val = targetValue;
		if (txList.size()>0){
			int txIndex = txList.size()-1 ; 						
			//if need more input to pay one ouput
			while (val > 0 ){
				Transaction preTx = getTransaction(txIndex);
				
				for (int preOutputIndex = 0 ;preOutputIndex<preTx.getOutputs().size();preOutputIndex++){
					Transaction.Output preOp = preTx.getOutput(preOutputIndex);
					if (preOp.address.equals(payerAddress) && val>0) {
						Map input= new HashMap();
						//targetTx.addInput(, preOutputIndex);
						input.put(C_PREHASH, preTx.getHash());
						input.put(C_PREINDEX, preOutputIndex);	
						input.put(C_PREVALUE, preOp.value);
						inputList.add(input);
						//calculate rest value
						val -= preOp.value;
					}
				}
				txIndex -- ; 
			}
			
		} 
		if (val>0) { // cannot find enough input value
			return null;
		} else {
			return inputList;
		}
	}
	public static Map<String, KeyPair> generateUser() {
		Map<String, KeyPair> map = new HashMap<String, KeyPair>();
		String[] users = { "Luke", "Phily", "Robert", "Lee", "Mason", "Henry", "Danny" };
		for (String user : users) {
			map.put(user, genKeyPair());
		}
		return map;

	}

	public static KeyPair genKeyPair() {
		KeyPairGenerator keyPairGenerator;

		try {
			keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024); // KeySize
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			return keyPair;
			// PrivateKey privateKey = keyPair.getPrivate();
			// PublicKey publicKey = keyPair.getPublic();
			// return publicKey;

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
	public void addToTxList(Transaction tx) {
		tx.finalize();
		txList.add(tx);
	}
	public static ArrayList<Transaction> genTxs(TxOperation txo) {

		txo.createCoins();

		String[][] trans = { { "Robert", "Mason", "50" }, { "Luke", "Lee", "30" }, { "Phily", "Henry", "20" },{ "Phily", "Danny", "40" } };

		Transaction tx = txo.addTransaction(trans);
		
		String[][] trans1 = { { "Luke", "Danny", "50" }, { "Phily", "Mason", "50" }, { "Lee", "Robert", "30" } };

		tx = txo.addTransaction(trans1);
		
		String[][] trans2 = { { "Phily", "Luke", "50" }, { "Robert", "Henry", "30" }, { "Mason", "Phily", "30" } };

		tx = txo.addTransaction(trans2);
		

		return txo.txList;
	}

	private Transaction addTransaction(String[][] trans) {
		//

		if (trans != null) {
			Transaction tx = new Transaction();
			for (String[] rec : trans) {
				double val = Double.parseDouble(rec[2]);
				//List<UTXO> inputList = txHdl.findTransactionInput(users.get(rec[0]).getPublic(), val,chain.toArray(new Transaction[chain.size()]));
				KeyPair payer =  users.get(rec[0]);
				KeyPair receiver =  users.get(rec[1]);
				//find from current transaction output, if receiver equals to payer, find value from their first  
				ArrayList<Transaction.Output> currentOutputs = (ArrayList<Transaction.Output>) tx.getOutputs().clone();
				for (Transaction.Output currentOP: currentOutputs){
					if (currentOP.address.equals(payer.getPublic())) {
						if (currentOP.value>val) {//enough value
							///currentOP.value -= val;	
							double v  = currentOP.value - val ; 
							tx.getOutputs().remove(currentOP);
							tx.addOutput(v, currentOP.address);
						} else {
							tx.getOutputs().remove(currentOP);
							val -= currentOP.value;
						}
						tx.addOutput(val, receiver.getPublic());
					}
				}
				
				//find input from previous input
				if (val >0 ) {
				List<Map> inputList = findInput(val,payer.getPublic());
				
				if (inputList != null) {
					double sumValue = 0 ; 
					for (Map inputMap:inputList) {
						double value = (double) inputMap.get(C_PREVALUE);
						tx.addInput((byte[])inputMap.get(C_PREHASH), (int)inputMap.get(C_PREINDEX));
						//byte[] message = tx.getRawDataToSign(tx.getInputs().size()-1);
						sumValue += value ; 
					}
					// pay to receiver
					tx.addOutput(val,  receiver.getPublic());
					if (sumValue>val) {
						// if not exact same , keep the change to payer
						tx.addOutput(sumValue - val, payer.getPublic());
					}
					//add signature
					
				} else {// not enough credit
					System.out.println("###ERROR: Cannot find correct input for " + rec[0] + " with value " + rec[2]);
				}
				}
				

			}
			//txHdl.completeTransaction(tx);
			
			addToTxList(tx);
			return tx;
		} else {
			return null;
		}

	}

	public String getUser(PublicKey pk) {
		for (String key : users.keySet()) {
			if (users.get(key).getPublic().equals(pk))
				return key;
		}
		return "";

	}

	public  byte[] genSignature(PrivateKey pKey, byte[] message) {
        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sig.initSign(pKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            sig.update(message);
            return sig.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return null;

    }
	public static void printInfo(TxOperation txo) {
		// print utxo pool
		ArrayList<Transaction>txs = txo.txList;
		System.out.println("############ UTXO POOL ###########");
		/*
		for (UTXO utxo : txo.txHdl.utxoPool.getAllUTXO()) {
			Transaction.Output op = txo.txHdl.utxoPool.getTxOutput(utxo);

			int i = 0;
			for (Transaction tx : txs) {
				UTXO ut = new UTXO(tx.getHash(), utxo.getIndex());
				if (ut.equals(utxo)) {
					System.out.print("tx" + i + ": ");
				}
				i++;
			}
			System.out.println(txo.getUser(op.address) + " " + utxo.getTxHash().hashCode() + " " + utxo.getIndex() + " "
					+ op.value);
		}
		*/
		// print transaction
		System.out.println("############ TRANSACTION ###########");
		int i = 0;
		for (Transaction tx : txs) {
			System.out.println("    ######## transaction " + i + " " +tx.hashCode()+ " ###########");
			System.out.println("    ====== OUTPUT ========");
			int index = 0;
			for (Transaction.Output txop : tx.getOutputs()) {
				System.out.println(index++ + ": " + txo.getUser(txop.address) + txop.value);

			}
			System.out.println("    ====== Input ========");
			for (Transaction.Input txip : tx.getInputs()) {
			
				
				Transaction tx1 = txo.findTransaction(txip.prevTxHash);
				if (tx1!=null) {
				Transaction.Output opOri = tx1.getOutput(txip.outputIndex);
				System.out.print("prev tx " + tx1.hashCode() + "(" + txip.outputIndex + "): " + txo.getUser(opOri.address) + " "
						+ opOri.value);
				if (txip.signature !=null) {
					System.out.print(" Signatured" );
				}
				System.out.println();
				}
			}
		
			i++;
		}
	
	}

	public static void main(String[] args) {
		Map map = generateUser();
		TxOperation txo = new TxOperation(map);
		ArrayList<Transaction> txs = genTxs(txo);
		printInfo(txo);
		System.out.println(txs.size());

	}

}
