import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class TxOperation {

	public static String C_PREHASH = "preHash";
	public static String C_PREINDEX = "preIndex";
	public static String C_PREVALUE = "preValue";
	private Map<String, KeyPair> users;
	private TxHandler txHdl = new TxHandler(new UTXOPool());
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
		tx.addOutput(50, users.get("Robert").getPublic());
		// Transaction[] txs = {tx};
		//txHdl.completeTransaction(tx);
		tx.finalize();
		Transaction[] orderedTxs = txHdl.handleTxs(new Transaction[]{tx});
		
		addToTxList(orderedTxs);
		return tx;
	}
	public Transaction getTransaction(int pos) {
		return txList.get(pos);
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
		
		txList.add(tx);
	}
	public void addToTxList(Transaction[] txs) {
		for (Transaction tx: txs){ 
			addToTxList(tx); 
		}
	}
	public static ArrayList<Transaction> genTxs(TxOperation txo) {

		ArrayList<Transaction> tList = new ArrayList<Transaction>();
		
		txo.createCoins();
		
		
		String[][] trans = { { "Mason", "80" },{ "Luke", "50" } };

		Transaction tx = txo.addTransaction("Robert",trans);
		tList.add(tx);
		String[][] trans1 = { { "Lee", "60" },{ "Henry", "10" } };

		 tx = txo.addTransaction("Phily",trans1);
		
		 tList.add(tx);
		 String[][] trans2 = { { "Danny", "40" },{ "Luke", "10" } };

		 tx = txo.addTransaction("Phily",trans2);
		 tList.add(tx);
		/*
		String[][] trans1 = { { "Luke", "Danny", "50" }, { "Phily", "Mason", "50" }, { "Lee", "Robert", "30" } };

		tx = txo.addTransaction(trans1);
		
		String[][] trans2 = { { "Phily", "Luke", "50" }, { "Robert", "Henry", "30" }, { "Mason", "Phily", "30" } };

		tx = txo.addTransaction(trans2);
		*/
		Transaction[] orderedTxs = txo.txHdl.handleTxs(tList.toArray(new Transaction[tList.size()]));
		txo.addToTxList(orderedTxs);
		return txo.txList;
	}

	private Transaction addTransaction(String user, String[][] trans) {
		//
		KeyPair payer =  users.get(user);
		if (trans != null) {
			
			UTXOPool userPool = txHdl.getUTXOByUser( payer.getPublic());
			Transaction tx = new Transaction();
			for (String[] rec : trans) {
				double val = Double.parseDouble(rec[1]);
				double outputVal = val ;
				KeyPair receiver =  users.get(rec[0]);
				
				//find input from user's utxo 
				for (UTXO ut:userPool.getAllUTXO()) {
					double inputVal = userPool.getTxOutput(ut).value;
					if (val >0) {
						//add input
						//if this UTXO is not used in current input
						if(!isUsedInCurrentInput(ut,tx.getInputs())){
							tx.addInput(ut.getTxHash(), ut.getIndex());
							//add signature
							
							val -= inputVal;
						}
					} else {
						break;
					}
				}
				if (val<=0){
					tx.addOutput(outputVal,  receiver.getPublic());
					if (val<0) {
						// if not exact same , keep the change to payer
						tx.addOutput(-val, payer.getPublic());
					}
					
					
					
				} else {// not enough credit
					System.out.println("###ERROR: Cannot find enough input for " + user + " to "+ rec[0]+ " with value " + rec[1]);
				}
				//add signature
				for (int ipIndex=0 ; ipIndex<tx.numInputs();ipIndex++){
					byte [] message = tx.getRawDataToSign(ipIndex);
					byte [] signature = genSignature(payer.getPrivate(), message);
					tx.addSignature(signature, ipIndex);
					
				}
				
				

			}
			//txHdl.completeTransaction(tx);
			tx.finalize();
			
			//addToTxList(tx);
			return tx;
		} else {
			return null;
		}

	}

	private boolean isUsedInCurrentInput(UTXO ut, ArrayList<Transaction.Input> inputs) {
		for (Transaction.Input ip :inputs){
			if(ut.equals(new UTXO(ip.prevTxHash,ip.outputIndex))){
				return true;
			}
		}
		return false; 
		
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
		
		// print transaction
		System.out.println("############ TRANSACTION ###########");
		int i = 0;
		for (Transaction tx : txs) {
			System.out.println("    ######## transaction " + i + " " +tx.hashCode()+ " ###########");
			System.out.println("    ====== OUTPUT ========");
			int index = 0;
			for (Transaction.Output txop : tx.getOutputs()) {
				System.out.println(index++ + ": " + txo.getUser(txop.address) +" " + txop.value);

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
		System.out.println("----------END--------------");

	}

}
