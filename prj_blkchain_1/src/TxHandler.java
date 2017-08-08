import java.awt.geom.GeneralPath;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

public class TxHandler {

	public UTXOPool utxoPool;
	
	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		this.utxoPool = utxoPool ; 
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are
	 *         valid, (3) no UTXO is claimed multiple times by {@code tx}, (4)
	 *         all of {@code tx}s output values are non-negative, and (5) the
	 *         sum of {@code tx}s input values is greater than or equal to the
	 *         sum of its output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		// TODO
		if (tx ==null ) {
			return false ;
		} 
		//(1) all outputs claimed by {@code tx} are in the current
		int opIndex = 0 ;		
		for (Transaction.Output op: tx.getOutputs()){
		
			UTXO utxo = new UTXO(tx.getHash(),opIndex);
			if (utxoPool.contains(utxo)){
				Transaction.Output utOp = utxoPool.getTxOutput(utxo);
				if (!(op.address.equals(utOp.address) && utOp.value == op.value) ) {
					return false ;
				} else {
					//continue to next output
				}
					
			} else {
				return false ; 
			}
			
			;
			opIndex ++ ; 
		}
		//(2) the signatures on each input of {@code tx} are  valid
		if (tx.getInputs() !=null ){
			int ipIndex = 0 ;
			for (Transaction.Input ip:tx.getInputs()){
				//find previous tx 
				Transaction.Output preOutput = utxoPool.getTxOutput(new UTXO(ip.prevTxHash,ip.outputIndex));
				if (!Crypto.verifySignature(preOutput.address,
						tx.getRawDataToSign(ipIndex), ip.signature))
						return false ;
				ipIndex ++ ; 
			}
		} //else input is empty means it's a create coin transaction , natually it's good
		return true ; 
		
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		// TODO
		return null;
	}
	public Transaction completeTransaction(Transaction tx) {
		// IMPLEMENT THIS
		tx.finalize();
		byte [] hash = tx.getHash();
		int index = 0 ;
		for (Transaction.Output op : tx.getOutputs()) {
			
			UTXO u = new UTXO(hash,index);
			utxoPool.addUTXO(u, op);			
			index ++;
			
		}
		for (Transaction.Input ip : tx.getInputs()) {
			
			UTXO utxo = new UTXO(ip.prevTxHash,ip.outputIndex);
			utxoPool.removeUTXO(utxo);
					
			index ++;
			
		}
		return tx;
	}
	public List<UTXO>findTransactionInput(PublicKey pk, double value, Transaction[] transactions) {
		
		List<UTXO> list = new ArrayList<UTXO> ();
		ArrayList<UTXO> allList = utxoPool.getAllUTXO();
		for (UTXO utxo : allList) {
			if (value <=0 ) break ; 
			Transaction.Output op = utxoPool.getTxOutput(utxo);
			if (op.address.equals(pk)) {
				Transaction prevTx = findTransaction(utxo.getTxHash(),transactions);
				//if (isValidTx(prevTx)) {					
					list.add(utxo);
					value -= op.value;
				//}
			}
		}
		// value > 0 means don't have enough credit to pay
		if (value >0) {
			return null ;
		} else {
			return list;
		}
	}

	public Transaction findTransaction(byte [] hashCode, Transaction[] txs){
		for (Transaction tx: txs){
			if (tx.hashCode()==hashCode.hashCode()) return tx; 
				
			
			if (tx.matchHash(hashCode)) {
				return tx;
			}
			
		}
		return null;
	}

	public double consumeUTXO(UTXO utxo) {
		double val = utxoPool.getTxOutput(utxo).value;
		//utxoPool.removeUTXO(utxo);
		return val ; 
	}
	public static ArrayList<byte []> messageList = new ArrayList();
	
	public static void main(String[] args) {
		
		Transaction transaction = genTx(6,null);
		for(int a = 0;a<6;a++) {
			boolean b = Crypto.verifySignature(transaction.getOutput(a).address, messageList.get(a), transaction.getInput(a).signature);
			System.out.println("input " +a +" is " +b);
		}
		messageList.clear();
		transaction.finalize();
		Transaction transaction1 = genTx(8,transaction);
		for(int a = 0;a<8;a++) {
			boolean b = Crypto.verifySignature(transaction1.getOutput(a).address,
					messageList.get(a),
					transaction1.getInput(a).signature);
			System.out.println("input " +a +" is " +b);
		}
	}
	public static Transaction genTx(int count,Transaction prevTx) {
		Transaction  tx=new Transaction();
		byte[] prevHash = null ; 
		if (prevTx !=null)
				prevHash = prevTx.getHash();
		for (int i=0;i<count;i++) {
			KeyPair keyPair = genKeyPair();
			double value = (i+1)*100 ;
			
				
				tx.addOutput(value, keyPair.getPublic());
				tx.addInput(prevHash,i);
				byte[] message = tx.getRawDataToSign(i);
				tx.addSignature(genSignature(keyPair.getPrivate(),message), i);
				messageList.add(message);
				//tx.finalize();
				//prevHash = tx.getHash();
		}
		return tx; 
		
		
	}
	public static KeyPair genKeyPair() {
		KeyPairGenerator keyPairGenerator;
	
			try {
				keyPairGenerator = KeyPairGenerator.getInstance("RSA");
				keyPairGenerator.initialize(1024); // KeySize
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				return keyPair;
				//PrivateKey privateKey = keyPair.getPrivate();
			//	PublicKey publicKey = keyPair.getPublic();
			//	return publicKey;

			
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
		
		
	}
	public static byte[] genSignature(PrivateKey pKey, byte[] message) {
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

	public Transaction findTransaction(byte[] prevTxHash, ArrayList<Transaction> txs) {
		
		return findTransaction(prevTxHash,txs.toArray(new Transaction[txs.size()]));
	}

}
