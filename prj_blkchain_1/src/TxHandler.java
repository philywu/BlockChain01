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
import java.util.stream.Collectors;

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
		//this is a create coin transaction
		if(tx.numInputs()==0){
			return true;
		}
		
		int ipIndex = 0 ;
		int sumInput = 0 ;
		int sumOutput = 0 ; 
		for (Transaction.Input input: tx.getInputs()){
			UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
			//(1) all outputs claimed by {@code tx} are in the current
			if (!utxoPool.contains(utxo)){
				return false;
			}
			
			
			//(2) the signatures on each input of {@code tx} are  valid
			Transaction.Output preOutput = utxoPool.getTxOutput(utxo);
			if (!Crypto.verifySignature(preOutput.address,
					tx.getRawDataToSign(ipIndex), input.signature))		
				return false ;
			//(3) no UTXO is claimed multiple times by {@code tx}
			if (this.usedUTXOPool.contains(utxo)){
				//if this UTXO has been claimed by previous tx, return false
				return false;
			} else {
				//if not claimed add to the claim list
				this.usedUTXOPool.addUTXO(utxo, preOutput);
			}
			
			sumInput += preOutput.value;
			ipIndex++;
		}
		for (Transaction.Output output : tx.getOutputs() ){
			double val = output.value;
			if (val <0){
				//(4) all of {@code tx}s output values are non-negative
				return false;
			}
			sumOutput+= val;
		}
		//(5) sum of {@code tx}s input values is greater than or equal to the
		//*         sum of its output values;
		if (sumInput<sumOutput){
			return false ; 
		}
		
		
		return true ; 
		
		
	}
	
	private UTXOPool usedUTXOPool ;

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		// TODO
		
		ArrayList<Transaction> txList = new ArrayList<Transaction> ();
		
		this.usedUTXOPool = new UTXOPool();
		
		//loop for each possible transactions
		for (Transaction tx:possibleTxs) {
			if (isValidTx(tx)) {
				//1. update UTXO pool 
				//remove input from pool
				if (tx.numInputs()>0) {// tx input = 0 means creat coin transaction
					for (Transaction.Input txInput: tx.getInputs()) {
						UTXO utxoOld = new UTXO(txInput.prevTxHash,txInput.outputIndex);
						if (utxoPool.contains(utxoOld)) {
							utxoPool.removeUTXO(utxoOld);
						}
					}
				}
				//add output to pool 
				int outputIndex =0 ;
				for (Transaction.Output txOuput:tx.getOutputs()) {
					UTXO utxoNew = new UTXO(tx.getHash(), outputIndex++ );
					utxoPool.addUTXO(utxoNew, txOuput);
				}
				//2. add to return list
				txList.add(tx);
				
			}
		}
		return txList.toArray(new Transaction[txList.size()]);
	}
	public UTXOPool getUTXOByUser(PublicKey user) {
		UTXOPool userPool = new UTXOPool();
		List<UTXO> utxoList = utxoPool.getAllUTXO();
		/*
		List<UTXO> filterList = utxoList.stream().filter(u -> {
			Transaction.Output op = utxoPool.getTxOutput(u);
			return ( op!=null && op.address.equals(user));
		}).collect(Collectors.toList());
		*/
		for (UTXO utxo: utxoList) {
			Transaction.Output op = utxoPool.getTxOutput(utxo);
			if (op.address.equals(user)) {
				userPool.addUTXO(utxo, op);
			}
		}
		
		
		return userPool; 
	}

}
