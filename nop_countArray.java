import java.util.*;

class nop_countArray{
  public static void main(String args[]){
    int C = 32; /*コア数*/
    int N = 256; /*中間層のニューロン数*/
    int k = N/2; /*新しい入力数*/
    int d = 2; /*分割数*/
    int clkCycle = 500000;
    String exeInst[][] = new String[C][clkCycle];

    String prog[] = {"PSM_i","PSM_s","PSM_f","PSM_o","MULv_i_s",
    "MULv_f_sB","ADDv_f_sD","TANHv_s","MULv_sD_o"};
    String in[] = {"IN_x","INADD_i","INADD_s","INADD_f","INADD_o","IN_h"};
    String out[] = {"OUT_i","OUT_s","OUT_f","OUT_o","OUT_h"};
    //String in[] = {"IN","IN","IN","IN","IN","IN"};
    //String out[] = {"OUT","OUT","OUT","OUT","OUT"};

    //各命令の実行状態を格納(0未実行状態 1実行可能状態への変更 2実行可能状態 3実行済み状態)
    int Pflag[][] = new int[C][prog.length];
    int Iflag[][] = new int[C][in.length];
    int Oflag[][] = new int[C][out.length];
    Iflag[0][0] = 2; //コア0のIN_x命令のみ実行可能状態に

    int iMid[][] = new int[3][C]; //in,out演算命令を各どこまで命令を実行したか保存しておく配列
    int didOpeflag[] = new int[C]; //実行された命令があったか(NOP判定に使用)
    int comflag = 0; //通信中は1に変更
    int ret = 0; //judgeメソッドの戻り値(実行可能な命令の通し番号)を保存

    long prog_count[] = {N*(N+k)/C, N*(N+k)/C, N*(N+k)/C, N*(N+k)/C,
      N/C, N/C, N/C, N/C, N/C};//演算命令の実行回数を格納した配列
    long in_count[] = {N/C, N/C, N/C, N/C, N/C, N/C};//IN命令の実行回数を格納した配列
    long out_count[] = {N/C, N/C, N/C, N/C, N/C};//IN命令の実行回数を格納した配列

    int timeStep = 0; //実行したタイムステップ数をカウント
    int nopCount = 0; //nop数のカウント
    double kadouritu = 0; //稼働率



    //実行開始
    for(int i=0; i<clkCycle; i++){
      comflag = 0; //初期化

      //各コアでの処理開始
      for(int doCore = 0; doCore < C; doCore++ ){
        ret = judge(doCore, Iflag); //実行可能なIN命令があればそのIN命令の番号を返す
        if(ret >= 0 && comflag == 0){
          //実行可能なIN命令がある
          comflag = 1;
          exeInst[doCore][i] = in[ret];
          didOpeflag[doCore] = 1; //命令を実行したのでフラグを１に変更
          iMid[0][doCore]++; //実行回数をインクリメント
          if(in_count[ret] == iMid[0][doCore]){
            //規定の実行回数に到達
            Iflag[doCore][ret] = 3; //終わった命令の状態フラグを3(実行済み)に変更
            iMid[0][doCore] = 0; //実行回数を０に戻す

            //終了した命令,コア番号によって状態フラグの変更を行う
            Iflag = cIFlag(C, d, doCore, 0, ret, Iflag);
            Oflag = cOFlag(C, d, doCore, 0, ret, Oflag);
            Pflag = cPFlag(C, d, doCore, 0, ret, Pflag);
          }
        }else{
          ret = judge(doCore, Oflag); //実行可能なIN命令があればそのIN命令の番号を返す
          if(ret >= 0 && comflag == 0){
            //実行可能なOUT命令がある
            comflag = 1;
            exeInst[doCore][i] = out[ret];
            didOpeflag[doCore] = 1; //命令を実行したのでフラグを１に変更
            iMid[1][doCore]++; //実行回数をインクリメント
            if(out_count[ret] == iMid[1][doCore]){
              //規定の実行回数に到達
              if(ret == 4 && doCore == C-1){//コア番号最大のコアでOUT_h命令が終了
                timeStep++;//タイムステップカウントをインクリメント
              }
              Oflag[doCore][ret] = 3; //終わった命令の状態フラグを3(実行済み)に変更
              iMid[1][doCore] = 0; //実行回数を０に戻す

              //終了した命令,コア番号によって状態フラグの変更を行う
              Iflag = cIFlag(C, d, doCore, 1, ret, Iflag);
              Oflag = cOFlag(C, d, doCore, 1, ret, Oflag);
              Pflag = cPFlag(C, d, doCore, 1, ret, Pflag);
            }
          }else{
            ret = judge(doCore, Pflag); //実行可能な演算命令があればそのIN命令の番号を返す
            if(ret >= 0){
              //実行可能な演算命令がある
              exeInst[doCore][i] = prog[ret];
              didOpeflag[doCore] = 1; //命令を実行したのでフラグを１に変更
              iMid[2][doCore]++; //実行回数をインクリメント
              if(prog_count[ret] == iMid[2][doCore]){
                //規定の実行回数に到達
                Pflag[doCore][ret] = 3; //終わった命令の状態フラグを3(実行済み)に変更
                iMid[2][doCore] = 0;//実行回数を０に戻す

                //終了した命令,コア番号によって状態フラグの変更を行う
                Iflag = cIFlag(C, d, doCore, 2, ret, Iflag);
                Oflag = cOFlag(C, d, doCore, 2, ret, Oflag);
                Pflag = cPFlag(C, d, doCore, 2, ret, Pflag);
              }
            }
          }
        }
      }


      //実行した命令が無ければnopを配列に格納
      for(int n = 0; n<C; n++){
        if(didOpeflag[n] == 0){
          exeInst[n][i] = "nop";
          nopCount++;
        }
        else didOpeflag[n] = 0;
      }

      //実行状態の状態1を実行可能状態(状態2に変更)
      Pflag = changExe(C,Pflag);
      Iflag = changExe(C,Iflag);
      Oflag = changExe(C,Oflag);

    }
    //稼働率の計算を行う
    kadouritu = kadouritu(C, nopCount, timeStep, prog_count, in_count, out_count);

    //実行した命令を表示
    //print(C, exeInst);

    //結果の表示String.format("%5d",n+1)
    System.out.println("\n\n"+"コア数C: "+C+" 中間層のニューロン数N: "+N+" 分割数: "+d+" の場合");

    System.out.print(N);
    System.out.print(" "+k);
    System.out.print(" "+timeStep);
    System.out.print(" "+nopCount);
    System.out.print(" "+kadouritu*100);

    //System.out.println("nop命令実行数は "+ nopCount);
    //System.out.println("LSTMを "+timeStep+" タイムステップ実行");
    //System.out.println("平均稼働率は "+kadouritu*100+" %");
  }

  //実行命令配列に保存された命令の全表示
  public static void print(int C, String[][] exeInst){
    for(int x = 0; x < C; x++){
      System.out.println("core" +x);
      for(int n = 0; n < exeInst[x].length; n++){
        System.out.println(exeInst[x][n]);
      }
    }
  }


  //実行可能な命令があるか判定しあればその命令の遠し番号を戻り値として返す
  public static int judge(int C, int[][] flag){
    int ret = -1;
    for(int i = 0; i<flag[C].length;i++){
      if(flag[C][i] == 2){
        ret = i;
        break;
      }
    }
    return ret;
  }

  //実行可能状態に変更可能な状態フラグの変更するメソッド
  public static int[][] changExe(int C, int[][] flagarray){
    for(int i = 0; i<flagarray.length; i++){
      for(int j = 0; j<flagarray[0].length; j++){
        if(flagarray[i][j] == 1){
          flagarray[i][j] = 2;
        }
      }
    }
    return flagarray;
  }

  //稼働率の算出
  public static double kadouritu(int C, int nop,  int timeStep, long[] prog, long[] in, long[] out){
    double kadouritu = 0;
    double sum = 0; //1タイムステップに実行する命令数(prog_count,in_count,out_count配列の各要素の合計)
    for(int i = 0; i<prog.length;i++){
      sum += prog[i];
    }
    for(int j = 0; j<in.length;j++){
      sum += in[j];
    }
    for(int k = 0; k<out.length;k++){
      sum += out[k];
    }
    if(timeStep != 0){
      kadouritu = sum / (sum + nop / (C*timeStep));
    }
    else{
      kadouritu = sum / (sum + nop / C);
    }
    return kadouritu;
  }

  //演算命令状態フラグの変更
  public static int[][] cPFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 0){ //IN命令
      if(finOpeNum == 0){ //IN_xの終了
        array[core][0] = 1; //PSM_i実行可に
        array[core][1] = 1; //PSM_s実行可に
        array[core][2] = 1; //PSM_f実行可に
        array[core][3] = 1; //PSM_o実行可に
      }
      else if(finOpeNum == 2) array[core][4]=1; //INADD_sの終了=>MULv_i_s実行可に
    }
    else if(finOpeType ==2){
      if(finOpeNum == 2) array[core][5]=1; //PSM_fの終了=>MULv_f_sB実行可に
      if(finOpeNum == 5) array[core][6]=1; //MULv_f_sBの終了=>ADDv_f_sD実行可に
      if(finOpeNum == 6) array[core][7]=1; //ADDv_f_sDの終了=>TANHv_s実行可に
      if(finOpeNum == 7) array[core][8]=1; //TANHv_sの終了=MULv_sD_o実行可に
    }
    return array;
  }

  //OUT命令状態フラグ変更
  public static int[][] cOFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 2){ //演算命令
      if(finOpeNum == 0) array[core][0] = 1; //PSM_iの終了=>OUT_i実行可に
      else if(finOpeNum == 1) array[core][1] = 1; //PSM_sの終了=>OUT_s実行可に
      else if(finOpeNum == 2) array[core][2] = 1; //PSM_fの終了=>OUT_f実行可に
      else if(finOpeNum == 3) array[core][3] = 1; //PSM_oの終了=>OUT_o実行可に
      else if(finOpeNum == 8) array[core][4] = 1; //MULv_sD_oの終了=>OUT_h実行可に
    }
    return array;
  }

  //IN命令状態フラグ変更
  public static int[][] cIFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 0 && finOpeNum ==0){ //IN_x命令の終了
      if(core < Threshold) array[core+Threshold][0] = 1; //IN_ｘの終了=>相手コアのIN_x実行可に
      else if(core >= Threshold && core != cMax-1)array[core-Threshold+1][0] = 1; //IN_ｘの終了=>相手コアのIN_x実行可に
    }
    else if(finOpeType == 1){ //OUT命令の終了
      if(finOpeNum == 0 && core < Threshold) array[core+Threshold][1] = 1; //OUT_i命令の終了=>相手コアのINADD_i実行可に
      else if(finOpeNum == 0 && core >= Threshold) array[core-Threshold][1] = 1; //OUT_i命令の終了=>相手コアのINADD_i実行可に
      else if(finOpeNum == 1 && core < Threshold) array[core+Threshold][2] = 1; //OUT_s命令の終了=>相手コアのINADD_s実行可に
      else if(finOpeNum == 1 && core >= Threshold) array[core-Threshold][2] = 1; //OUT_s命令の終了=>相手コアのINADD_s実行可に
      else if(finOpeNum == 2 && core < Threshold) array[core+Threshold][3] = 1; //OUT_f命令の終了=>相手コアのINADD_f実行可に
      else if(finOpeNum == 2 && core >= Threshold) array[core-Threshold][3] = 1; //OUT_f命令の終了=>相手コアのINADD_f実行可に
    }
    if(finOpeType == 1 && finOpeNum == 4 && core == 2) array[0][0] = 1;//core2のOUT命令が終了=>core0のIN_x実行可能に
    return array;
  }
}
