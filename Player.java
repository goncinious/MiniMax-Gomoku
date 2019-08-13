import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
//import java.util.Comparator;

/** Minimax algorithm with alpha-beta pruning.
 *	The program implements custom heuristic function based on number of consecutive pieces on the
 *  board (ie. number of sets), the set lengths, number of open-ends per set and the player's turn.
 *  The final evaluation score is obtained by subtracting the evaluation obtained from the prespective
 *  of the min to the max player: eval(max) - eval(min). (Function ranging from -inf to +inf).

 *  In order to reduce the search space and make the pruning procedure more efficient,the moves were filtered according to the proximity
 *	to the pieces already positioned on the board.
 *	Other strategies were explored to improve the algorithm, such as  using iterative deepening, filtering bad moves and sorting
 *  the moves according to proximity to player's pieces. In addition, a  time-based cut-off was also implemented( ie. stopping search under the 10s constrain).
 *  Finally, 2 forced moves where also hard-coded, such as ending the game or blocking an opponent's winning move.
 *  More details are provided in the report and in the commented code.
 *	This code was tested in the ITL machines and everything was working fine.
 **/

class Player extends GomokuPlayer {

	public static final int MAX_SCORE = 100000000; // max score possible is +-20*MAX_SCORE used to represent win/loss - 5pieces in a row
	public static Color MaxPlayer;
	public static Color MinPlayer;
	public static final int depth_cutoff = 8;
	public static final double time_limit = 9.9;

	public Move chooseMove(Color[][] board, Color me) {

		// initialise variables
		long tStart = System.currentTimeMillis(); // start timer

		// set player's colors
	 	MaxPlayer = me;
		if (MaxPlayer.equals(Color.WHITE)){
			MinPlayer = Color.BLACK;
		}else{
			MinPlayer = Color.WHITE;
	 }

	int alpha = -Integer.MAX_VALUE; // alpha-beta init values
	int beta = Integer.MAX_VALUE;

	// moves are stored as integer arrays of length 4 in the following way: [x_pos, y_pos, score, time_excceded?]
	int[] this_move = new int[4]; // store move at current depth (ICS)
	int[] final_move = new int[4]; // store final move executed
	// initialise final move
	final_move[0] = -1; // no move yet
	final_move[1] = -1;
	final_move[2] = -Integer.MAX_VALUE; // worst case
	final_move[3] = 0; // time limit still not exceeded

	// Iterative deepening - this is executed until time is exceeded or depth cut-off is reached
	for (int depth = 1; depth < depth_cutoff; depth++) {

		if(depth==1){this_move = MaxPlayerAB(board, alpha, beta, depth, tStart, true); // true variable to indicate searching for forced moves (only performed at the first level of the search)
		}else{this_move = MaxPlayerAB(board, alpha, beta, depth, tStart, false);}

		if(this_move[3]==0 && this_move[2]!=-20*MAX_SCORE){ // if time has not exceeded and its not a gameover move
			final_move = this_move; // update final move
			if (final_move[2] == 20*MAX_SCORE){;break;} // this is used to signal forced moves - finish game or block opponent from winning
		}

	}

	return new Move(final_move[0], final_move[1]); // execute final move
	} // chooseMove()


	public static boolean isGameOver(Color[][] board, Color turn) {
		/** Check if its terminal condition.
		 **/
		// get evaluation
		int score = getGlobalScore(board, turn);

		return(
			score == 20*MAX_SCORE ||    // win (+-20*MAX_SCORE used to represent win/loss - 5pieces in a row)
			score == -20*MAX_SCORE ||   // loss
			(Math.abs(score) != 20*MAX_SCORE && getFiltMoves(board,turn).size() == 0)  // draw
			);

	}

	public static int[] MaxPlayerAB(Color[][] board, int alpha, int beta, int depth, long tStart, boolean first_level) {
		/** Max player with alpha-beta pruning
		 **/

		// Initialise best move and score
		int[] best_move = new int[4]; // store [pos x, pos y, score]
		best_move[0] = -1; // initialise best move to (-1,-1) - no move yet
		best_move[1] = -1;
		best_move[3] = 0; // time not exceeded


		// Terminal state condition
		double elapsedSeconds = (System.currentTimeMillis() - tStart) / 1000.0;
		if(depth == 0 || elapsedSeconds>=time_limit || isGameOver(board, MaxPlayer)) { // check if depth cut-off reached or time elapsed or is terminal state
			if(elapsedSeconds>=time_limit){best_move[3] = 1;} // set time limit = 1 (time exceeded)
			best_move[2] = getGlobalScore(board, MaxPlayer);
			return best_move;
		}

		// Get possible filtered moves from current state of the board
		ArrayList<Position> moves = getFiltMoves(board, MaxPlayer);

		// Initialise score
		best_move[2] = -Integer.MAX_VALUE; //  MAX player worst-case

		// Find best children state
		for(Position move : moves) {

			//		check if its forced play
					if(first_level){ // first level
						board[move.row][move.col] = MaxPlayer;
						if(getScore(board, MaxPlayer, MaxPlayer)==20*MAX_SCORE) {
							best_move[0] = move.row;
							best_move[1] = move.col;
							best_move[2] = 20*MAX_SCORE;
							return best_move;}

						board[move.row][move.col] = MinPlayer;
						if(getScore(board, MinPlayer, MaxPlayer)==20*MAX_SCORE) {
							best_move[0] = move.row;
							best_move[1] = move.col;
							best_move[2] = 20*MAX_SCORE;
							return best_move;}; // prevent game over
							board[move.row][move.col] = null;

					} //forced play

			//System.out.println("move considered: (" + move.row + ","+move.col+")");
				board[move.row][move.col] = MaxPlayer; // test children state
				int[] this_move = MinPlayerAB(board, alpha, beta, depth-1, tStart,first_level); // min - go down another level
				board[move.row][move.col] = null; // undo move

				if (this_move[2] > best_move[2]){ // if higher -> replace
					best_move[0] = move.row;
					best_move[1] = move.col;
					best_move[2] = this_move[2];
					best_move[3] = this_move[3];
				}
				if(this_move[2]>=beta){return this_move;} // alpha-beta pruning - check upper limit; if ilegal, return move (cutoff)
				if(this_move[2]>alpha){alpha = this_move[2];}  // alpha-beta pruning (update lower bound [alpha,beta])
			}
			return best_move;
		}

		public static int[] MinPlayerAB(Color[][] board, int alpha, int beta, int depth, long tStart,boolean first_level) {

			// Initialise best move and score
			int[] best_move = new int[5]; // store [pos x, pos y, score]
			best_move[0] = -1; // initialise best move to (-1,-1) - no move yet
			best_move[1] = -1;
			best_move[3] = 0;

			// Terminal state condition
			double elapsedSeconds = (System.currentTimeMillis() - tStart) / 1000.0;
			if(depth == 0 || elapsedSeconds>=time_limit || isGameOver(board, MinPlayer)) {
				if(elapsedSeconds>=time_limit){best_move[3] = 1;}
				best_move[2] = getGlobalScore(board, MinPlayer);
				return best_move;
			}
			// Get possible moves from current state of the board
			ArrayList<Position> moves = getFiltMoves(board,MinPlayer);

			// Initialise score
			best_move[2] = Integer.MAX_VALUE; //  MIN player worst-case

			// Find best children
			for(Position move : moves) {

					board[move.row][move.col] = MinPlayer; // test children state
					int[] this_move = MaxPlayerAB(board, alpha, beta, depth-1, tStart, first_level); // max - go down another level
					board[move.row][move.col] = null;

					if (this_move[2] < best_move[2]){
						best_move[0] = move.row;
						best_move[1] = move.col;
						best_move[2] = this_move[2];
						best_move[3] = this_move[3];
					}
					if(this_move[2]<=alpha){return this_move;} // alpha-beta pruning - check lower limit; if ilegal, return move (cutoff)
					if(this_move[2]<beta){beta = this_move[2];} // alpha-beta pruning (update lower bound [alpha,beta])
				}

				return best_move;
			}

	public static int getGlobalScore(Color[][] board, Color turn) {
		/** Get gobal score by subtracting min eval to the max eval.
		 **/

		int score_max = getScore(board, MaxPlayer, turn); // relative to max player
		int score_min = getScore(board, MinPlayer, turn);

			if (score_max==MAX_SCORE*20) { // signal a win for max
				return score_max;

			} else if (score_min==MAX_SCORE*20) { // signal a win for min
				return -score_min;

			} else { // subtract both scores
				return score_max - score_min;
			}
	}

	public static int getScore(Color[][] board, Color piece, Color turn) {
		/** Get evaluation for color of piece considering that is color turn.
		 **/

		 // Variables
		 // Horizontal
			List<Integer> H = new ArrayList<Integer>(); // keep track of activated rows (to keep track of multiple sets in horizontal direction)
			int set_extend_h = -1; // keep track of set extends (length of sets) - for cases with multiple sets in a row

		 // Vertical
			List<Integer> V = new ArrayList<Integer>();
			int[] set_extend_v = new int[8]; // save for each column

		 // Diagonal L->R
			List<Integer> diagLR = new ArrayList<Integer>();
			int[][] set_extend_diagLR = new int[15][2]; // 15-max number of diag that you can have in 8x8; 2 - xy coordinates

		 // Diagonal R->L
			List<Integer> diagRL = new ArrayList<Integer>();
			int[][] set_extend_diagRL = new int[15][2];

			int score_sum = 0; // cumulative score
			int score; // partial score
			int Consecutive; // number of consecutive pieces (set length)

			for (int row = 0; row < board.length; row++) {
					for (int col = 0; col < board[row].length; col++) {

							if (board[row][col]!=null && board[row][col].equals(piece)){ // for each player's pieces

								// HORIZONTAL (1: if its piece and has not be activated in that row OR 2: activated but its another set (multiple sets per rows))
								if(!H.contains(row) || (H.contains(row) && col>set_extend_h))
								{
								  Consecutive = CountPieces(board,row,col, 0, 1, piece); // get set length (direction: 0 vertical, 1 horizontal)
								  score = EvaluateHorizontal(board, row, col, Consecutive, piece, turn); // get score
									if(score==MAX_SCORE*20){return score;} else{score_sum+=score;} // if its win condition (5 piece in a row), return; else sum score
								  H.add(row); // add to activated rows
								  set_extend_h = col+Consecutive; // col index of the last piece of the set
								} // horizontal

								// VERTICAL
								if(!V.contains(col) || (V.contains(col) && row>set_extend_v[col]))
								{
								  Consecutive = CountPieces(board,row,col,1,0,piece);
								  score = EvaluateVertical(board, row, col, Consecutive, piece, turn);
									if(score==MAX_SCORE*20){return score;} else{score_sum+=score;}
								  V.add(col);
								  set_extend_v[col] = row+Consecutive;
								} // vertical

								// DIAGONAL (L-R) row-col is the same for every element of LR diagonal
			          if(!diagLR.contains(row-col+7) || (diagLR.contains(row-col+7) && row>set_extend_diagLR[row-col+7][0] && col>set_extend_diagLR[row-col+7][1]) )
			          {
			            Consecutive = CountPieces(board,row,col,1,1,piece);
			            score = EvaluateDiagonalLR(board, row, col, Consecutive, piece, turn);
									if(score==MAX_SCORE*20){return score;} else{score_sum+=score;}
			            diagLR.add(row-col+7); // +7 - so i get index from 0-14
			            set_extend_diagLR[row-col+7][0] = row+Consecutive;
			            set_extend_diagLR[row-col+7][1] = col+Consecutive;
			          } // diagLR

								// DIAGONAL (R-L) row+col is the same for every element of RL diagonal
								if(!diagRL.contains(row+col) || (diagRL.contains(row+col) && row>set_extend_diagRL[row+col][0] && col<set_extend_diagRL[row+col][1]) )
								{
									Consecutive = CountPieces(board,row,col,1,-1, piece);
									score = EvaluateDiagonalRL(board, row, col, Consecutive, piece, turn);
									if(score==MAX_SCORE*20){return score;} else{score_sum+=score;}
									diagRL.add(row+col);
									set_extend_diagRL[row+col][0] = row+Consecutive;
									set_extend_diagRL[row+col][1] = col-Consecutive+1;
								} // diagLR



							}// if not null

						} // forloop col
					}// forloop row

		return score_sum;
	}

	public static int CountPieces(Color[][] board, int row, int col, int rowd, int cold, Color player) {
		/**
 * Counts number of connected player' pieces in a row: starting from (row, col) and moving in direction (rowd, cold).
 */
	  int count = 0;
	  for (int i = 0; i < 5; i++) {
	    if((row + i * rowd)>=0 && (row + i * rowd)<8 && (col + i * cold)>=0 && (col + i * cold)<8){ // position is within grid
	      if (board[row + i * rowd][col + i * cold] == player) count++;
	      else break;
	    }
	  }
	  return count;
	}

	public static int CountOpen(Color[][] board, int row, int col, int rowd, int cold) {
		/**
 * Checks if there is an open end in the position [row+rowd, col+cold]
 */
	  int count = 0;
	    if((row + rowd)>=0 && (row + rowd)<8 && (col + cold)>=0 && (col + cold)<8){ // position is within grid
	        if (board[row + rowd][col + cold] == null) count++;
	  }
	  return count;
	}

	public static int NotEmpty(Color[][] board, int row, int col, int rowd, int cold) {
		/**
		* Checks if there is a piece in the position [row+rowd, col+cold]
 */
			if((row + rowd)>=0 && (row + rowd)<8 && (col + cold)>=0 && (col + cold)<8){ // position is within grid
				if (board[row +  rowd][col + cold] != null){return 1;};
			}
		return 0;
	}


	public static class Position {
		/** Class to represent position on the board
		 **/
		public int row;
		public int col;
	//	public int w; // weight - used to test sorting
		public Position(int row, int col) {
			this.row = row;
			this.col = col;
		//	this.w = w;
		}
	}

	public static ArrayList<Position> getFiltMoves(Color[][] board, Color turn) {
		ArrayList<Position> availableMoves = new ArrayList<Position>();

		for (int row = 0; row < board.length; row++) {
			for (int col = 0; col < board[row].length; col++) {

				if (board[row][col] == null) {

					// check if its not an isolated position (no piece around 1 position)
					if(NotEmpty(board, row, col, 1, 0)==1){availableMoves.add(new Position(row, col)); // down
					}else if(NotEmpty(board, row, col, -1, 0)==1){availableMoves.add(new Position(row, col)); //up
					}else if(NotEmpty(board, row, col, 0, 1)==1){availableMoves.add(new Position(row, col)); // R
					}else if(NotEmpty(board, row, col, 0, -1)==1){availableMoves.add(new Position(row, col)); // L
					}else if(NotEmpty(board, row, col, 1, 1)==1){availableMoves.add(new Position(row, col)); // diag LR down
					}else if(NotEmpty(board, row, col, -1, -1)==1){availableMoves.add(new Position(row, col)); // diag LR up
					}else if(NotEmpty(board, row, col, 1, -1)==1){availableMoves.add(new Position(row, col)); // diag RL down
					}else if(NotEmpty(board, row, col, -1, 1)==1){availableMoves.add(new Position(row, col)); // diag RL up
					}else{continue;}

					// weighted version
					// int weight = 0;
					// weight += NotEmptyWeighted(board,row,col,1,0,turn); //down
					// weight += NotEmptyWeighted(board, row, col, -1, 0,turn); //up
					// weight += NotEmptyWeighted(board, row, col, 0, 1,turn);//R
					// weight += NotEmptyWeighted(board, row, col, 0, -1,turn);//L
					// weight += NotEmptyWeighted(board,row,col,1,1,turn);//diag LR down
					// weight += NotEmptyWeighted(board,row,col,-1,-1,turn);//diag LR up
					// weight += NotEmptyWeighted(board,row,col,1,-1,turn);// diag RL down
					// weight += NotEmptyWeighted(board,row,col,-1,1,turn); //diag RL up
					//
					// if(weight>0){
					// 	availableMoves.add(new Location(row, col,weight));
					// }
				}

			}
		}

		if(availableMoves.size()==0 && board[3][3]==null){ // board is empty (first move) - also taking into account draw scenario
			availableMoves.add(new Position(3, 3)); // position it on the centre of the board
		}

// weighted version
// {else{
		// 	availableMoves.sort(Comparator.comparing(a -> a.w)); //sort filtered moves by weights (ascending order)
		// }

		return availableMoves;
	}

// Weighted version for sorting
	// public static int NotEmptyWeighted(Color[][] board, int row, int col, int rowd, int cold, Color player) {
	//
	// 	for (int i = 1; i < 3; i++) {
	// 		if((row + i * rowd)>=0 && (row + i * rowd)<8 && (col + i * cold)>=0 && (col + i * cold)<8){ // position is within grid
	//
	// 			if (board[row + i * rowd][col + i * cold] == player && i==1){return 1;
	// 			}else if (board[row + i * rowd][col + i * cold] == player && i==2) {return 3;
	// 			}else if(board[row + i * rowd][col + i * cold] != player && board[row + i * rowd][col + i * cold] != null && i==1){return 30;
	// 			}else if(board[row + i * rowd][col + i * cold] != player && board[row + i * rowd][col + i * cold] != null && i==2){return 50;
	// 			}else{continue;}
	//
	// 		}
	// 	}
	// 	return 0;
	// }


	public static int EvaluateHorizontal(Color[][] board, int row, int col, int Consecutive, Color player, Color turn) {
	  // horizontal
	  int openEnds = CountOpen(board,row,col,0,-1) + CountOpen(board,row,col+Consecutive-1,0,1); // count number of open ends on both sides

	  switch(Consecutive){ // set length
	    case 5: return MAX_SCORE*20;
	    case 4: switch(openEnds){ // number of open-ends
	              case 2: if(turn.equals(player)){return MAX_SCORE;} else{return 500000;}
	              case 1: if(turn.equals(player)){return MAX_SCORE;} else{return 50;}
	            } break;
	    case 3: switch(openEnds){
	              case 2: if(turn.equals(player)){return 10000;} else{return 50;}
	              case 1: if(turn.equals(player)){return 10;} else{return 7;}
	            } break;
	    case 2: switch(openEnds){
	              case 2: return 7;
	              case 1: return 3;
	            } break;
	    case 1: switch(openEnds){
	              case 2: return 2;
	              case 1: return 1;
	            } break; // switch openEnds
	          } // switch Consecutive
	          return 0;
	}

	public static int EvaluateVertical(Color[][] board, int row, int col, int Consecutive, Color player, Color turn) {
		// Vertical
		int openEnds = CountOpen(board,row,col,-1,0) + CountOpen(board,row+Consecutive-1,col,1,0);

		switch(Consecutive){
			case 5: return MAX_SCORE*20;
			case 4: switch(openEnds){
								case 2: if(turn.equals(player)){return MAX_SCORE;} else{return 500000;}
								case 1: if(turn.equals(player)){return MAX_SCORE;} else{return 50;}
							} break;
			case 3: switch(openEnds){
								case 2: if(turn.equals(player)){return 10000;} else{return 50;}
								case 1: if(turn.equals(player)){return 10;} else{return 7;}
							} break;
			case 2: switch(openEnds){
								case 2: return 7;
								case 1: return 3;
							} break;
			case 1: switch(openEnds){
								case 2: return 2;
								case 1: return 1;
							} break; // switch openEnds
						} // switch Consecutive
						return 0;
	}

	public static int EvaluateDiagonalLR(Color[][] board, int row, int col, int Consecutive, Color player, Color turn) {
		// Diagonal LR
		int openEnds = CountOpen(board,row,col,-1,-1) + CountOpen(board,row+Consecutive-1,col+Consecutive-1,1,1);

		switch(Consecutive){
			case 5: return MAX_SCORE*20;
			case 4: switch(openEnds){
								case 2: if(turn.equals(player)){return MAX_SCORE;} else{return 500000;}
								case 1: if(turn.equals(player)){return MAX_SCORE;} else{return 50;}
							} break;
			case 3: switch(openEnds){
								case 2: if(turn.equals(player)){return 10000;} else{return 50;}
								case 1: if(turn.equals(player)){return 10;} else{return 7;}
							} break;
			case 2: switch(openEnds){
								case 2: return 7;
								case 1: return 3;
							} break;
			case 1: switch(openEnds){
								case 2: return 2;
								case 1: return 1;
							} break; // switch openEnds
						} // switch Consecutive
						return 0;
	}

	public static int EvaluateDiagonalRL(Color[][] board, int row, int col, int Consecutive, Color player, Color turn) {
		// Diagonal RL
		int openEnds = CountOpen(board,row,col,-1,1) + CountOpen(board,row+Consecutive-1,col-Consecutive+1,1,-1);

		switch(Consecutive){
			case 5: return MAX_SCORE*20;
			case 4: switch(openEnds){
								case 2: if(turn.equals(player)){return MAX_SCORE;} else{return 500000;}
								case 1: if(turn.equals(player)){return MAX_SCORE;} else{return 50;}
							} break;
			case 3: switch(openEnds){
								case 2: if(turn.equals(player)){return 10000;} else{return 50;}
								case 1: if(turn.equals(player)){return 10;} else{return 7;}
							} break;
			case 2: switch(openEnds){
								case 2: return 7;
								case 1: return 3;
							} break;
			case 1: switch(openEnds){
								case 2: return 2;
								case 1: return 1;
							} break; // switch openEnds
						} // switch Consecutive
						return 0;
	}

}
