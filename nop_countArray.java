import java.util.*;

// git test
class nop_countArray{
  public static void main(String args[]){
    int C = 32; /*�R�A��*/
    int N = 256; /*���ԑw�̃j���[������*/
    int k = N/2; /*�V�������͐�*/
    int d = 2; /*������*/
    int clkCycle = 500000;
    String exeInst[][] = new String[C][clkCycle];

    String prog[] = {"PSM_i","PSM_s","PSM_f","PSM_o","MULv_i_s",
    "MULv_f_sB","ADDv_f_sD","TANHv_s","MULv_sD_o"};
    String in[] = {"IN_x","INADD_i","INADD_s","INADD_f","INADD_o","IN_h"};
    String out[] = {"OUT_i","OUT_s","OUT_f","OUT_o","OUT_h"};
    //String in[] = {"IN","IN","IN","IN","IN","IN"};
    //String out[] = {"OUT","OUT","OUT","OUT","OUT"};

    //�e���߂̎��s���Ԃ��i�[(0�����s���� 1���s�\���Ԃւ̕ύX 2���s�\���� 3���s�ςݏ���)
    int Pflag[][] = new int[C][prog.length];
    int Iflag[][] = new int[C][in.length];
    int Oflag[][] = new int[C][out.length];
    Iflag[0][0] = 2; //�R�A0��IN_x���߂̂ݎ��s�\���Ԃ�

    int iMid[][] = new int[3][C]; //in,out���Z���߂��e�ǂ��܂Ŗ��߂����s�������ۑ����Ă����z��
    int didOpeflag[] = new int[C]; //���s���ꂽ���߂���������(NOP�����Ɏg�p)
    int comflag = 0; //�ʐM����1�ɕύX
    int ret = 0; //judge���\�b�h�̖߂��l(���s�\�Ȗ��߂̒ʂ��ԍ�)���ۑ�

    long prog_count[] = {N*(N+k)/C, N*(N+k)/C, N*(N+k)/C, N*(N+k)/C,
      N/C, N/C, N/C, N/C, N/C};//���Z���߂̎��s�񐔂��i�[�����z��
    long in_count[] = {N/C, N/C, N/C, N/C, N/C, N/C};//IN���߂̎��s�񐔂��i�[�����z��
    long out_count[] = {N/C, N/C, N/C, N/C, N/C};//IN���߂̎��s�񐔂��i�[�����z��

    int timeStep = 0; //���s�����^�C���X�e�b�v�����J�E���g
    int nopCount = 0; //nop���̃J�E���g
    double kadouritu = 0; //�ғ���



    //���s�J�n
    for(int i=0; i<clkCycle; i++){
      comflag = 0; //������

      //�e�R�A�ł̏����J�n
      for(int doCore = 0; doCore < C; doCore++ ){
        ret = judge(doCore, Iflag); //���s�\��IN���߂������΂���IN���߂̔ԍ����Ԃ�
        if(ret >= 0 && comflag == 0){
          //���s�\��IN���߂�����
          comflag = 1;
          exeInst[doCore][i] = in[ret];
          didOpeflag[doCore] = 1; //���߂����s�����̂Ńt���O���P�ɕύX
          iMid[0][doCore]++; //���s�񐔂��C���N�������g
          if(in_count[ret] == iMid[0][doCore]){
            //�K���̎��s�񐔂ɓ��B
            Iflag[doCore][ret] = 3; //�I���������߂̏��ԃt���O��3(���s�ς�)�ɕύX
            iMid[0][doCore] = 0; //���s�񐔂��O�ɖ߂�

            //�I����������,�R�A�ԍ��ɂ����ď��ԃt���O�̕ύX���s��
            Iflag = cIFlag(C, d, doCore, 0, ret, Iflag);
            Oflag = cOFlag(C, d, doCore, 0, ret, Oflag);
            Pflag = cPFlag(C, d, doCore, 0, ret, Pflag);
          }
        }else{
          ret = judge(doCore, Oflag); //���s�\��IN���߂������΂���IN���߂̔ԍ����Ԃ�
          if(ret >= 0 && comflag == 0){
            //���s�\��OUT���߂�����
            comflag = 1;
            exeInst[doCore][i] = out[ret];
            didOpeflag[doCore] = 1; //���߂����s�����̂Ńt���O���P�ɕύX
            iMid[1][doCore]++; //���s�񐔂��C���N�������g
            if(out_count[ret] == iMid[1][doCore]){
              //�K���̎��s�񐔂ɓ��B
              if(ret == 4 && doCore == C-1){//�R�A�ԍ��ő��̃R�A��OUT_h���߂��I��
                timeStep++;//�^�C���X�e�b�v�J�E���g���C���N�������g
              }
              Oflag[doCore][ret] = 3; //�I���������߂̏��ԃt���O��3(���s�ς�)�ɕύX
              iMid[1][doCore] = 0; //���s�񐔂��O�ɖ߂�

              //�I����������,�R�A�ԍ��ɂ����ď��ԃt���O�̕ύX���s��
              Iflag = cIFlag(C, d, doCore, 1, ret, Iflag);
              Oflag = cOFlag(C, d, doCore, 1, ret, Oflag);
              Pflag = cPFlag(C, d, doCore, 1, ret, Pflag);
            }
          }else{
            ret = judge(doCore, Pflag); //���s�\�ȉ��Z���߂������΂���IN���߂̔ԍ����Ԃ�
            if(ret >= 0){
              //���s�\�ȉ��Z���߂�����
              exeInst[doCore][i] = prog[ret];
              didOpeflag[doCore] = 1; //���߂����s�����̂Ńt���O���P�ɕύX
              iMid[2][doCore]++; //���s�񐔂��C���N�������g
              if(prog_count[ret] == iMid[2][doCore]){
                //�K���̎��s�񐔂ɓ��B
                Pflag[doCore][ret] = 3; //�I���������߂̏��ԃt���O��3(���s�ς�)�ɕύX
                iMid[2][doCore] = 0;//���s�񐔂��O�ɖ߂�

                //�I����������,�R�A�ԍ��ɂ����ď��ԃt���O�̕ύX���s��
                Iflag = cIFlag(C, d, doCore, 2, ret, Iflag);
                Oflag = cOFlag(C, d, doCore, 2, ret, Oflag);
                Pflag = cPFlag(C, d, doCore, 2, ret, Pflag);
              }
            }
          }
        }
      }


      //���s�������߂���������nop���z���Ɋi�[
      for(int n = 0; n<C; n++){
        if(didOpeflag[n] == 0){
          exeInst[n][i] = "nop";
          nopCount++;
        }
        else didOpeflag[n] = 0;
      }

      //���s���Ԃ̏���1�����s�\����(����2�ɕύX)
      Pflag = changExe(C,Pflag);
      Iflag = changExe(C,Iflag);
      Oflag = changExe(C,Oflag);

    }
    //�ғ����̌v�Z���s��
    kadouritu = kadouritu(C, nopCount, timeStep, prog_count, in_count, out_count);

    //���s�������߂��\��
    //print(C, exeInst);

    //���ʂ̕\��String.format("%5d",n+1)
    System.out.println("\n\n"+"�R�A��C: "+C+" ���ԑw�̃j���[������N: "+N+" ������: "+d+" �̏ꍇ");

    System.out.print(N);
    System.out.print(" "+k);
    System.out.print(" "+timeStep);
    System.out.print(" "+nopCount);
    System.out.print(" "+kadouritu*100);

    //System.out.println("nop���ߎ��s���� "+ nopCount);
    //System.out.println("LSTM�� "+timeStep+" �^�C���X�e�b�v���s");
    //System.out.println("���ωғ����� "+kadouritu*100+" %");
  }

  //���s���ߔz���ɕۑ����ꂽ���߂̑S�\��
  public static void print(int C, String[][] exeInst){
    for(int x = 0; x < C; x++){
      System.out.println("core" +x);
      for(int n = 0; n < exeInst[x].length; n++){
        System.out.println(exeInst[x][n]);
      }
    }
  }


  //���s�\�Ȗ��߂����邩���肵�����΂��̖��߂̉����ԍ����߂��l�Ƃ��ĕԂ�
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

  //���s�\���ԂɕύX�\�ȏ��ԃt���O�̕ύX���郁�\�b�h
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

  //�ғ����̎Z�o
  public static double kadouritu(int C, int nop,  int timeStep, long[] prog, long[] in, long[] out){
    double kadouritu = 0;
    double sum = 0; //1�^�C���X�e�b�v�Ɏ��s���閽�ߐ�(prog_count,in_count,out_count�z���̊e�v�f�̍��v)
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

  //���Z���ߏ��ԃt���O�̕ύX
  public static int[][] cPFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 0){ //IN����
      if(finOpeNum == 0){ //IN_x�̏I��
        array[core][0] = 1; //PSM_i���s��
        array[core][1] = 1; //PSM_s���s��
        array[core][2] = 1; //PSM_f���s��
        array[core][3] = 1; //PSM_o���s��
      }
      else if(finOpeNum == 2) array[core][4]=1; //INADD_s�̏I��=>MULv_i_s���s��
    }
    else if(finOpeType ==2){
      if(finOpeNum == 2) array[core][5]=1; //PSM_f�̏I��=>MULv_f_sB���s��
      if(finOpeNum == 5) array[core][6]=1; //MULv_f_sB�̏I��=>ADDv_f_sD���s��
      if(finOpeNum == 6) array[core][7]=1; //ADDv_f_sD�̏I��=>TANHv_s���s��
      if(finOpeNum == 7) array[core][8]=1; //TANHv_s�̏I��=MULv_sD_o���s��
    }
    return array;
  }

  //OUT���ߏ��ԃt���O�ύX
  public static int[][] cOFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 2){ //���Z����
      if(finOpeNum == 0) array[core][0] = 1; //PSM_i�̏I��=>OUT_i���s��
      else if(finOpeNum == 1) array[core][1] = 1; //PSM_s�̏I��=>OUT_s���s��
      else if(finOpeNum == 2) array[core][2] = 1; //PSM_f�̏I��=>OUT_f���s��
      else if(finOpeNum == 3) array[core][3] = 1; //PSM_o�̏I��=>OUT_o���s��
      else if(finOpeNum == 8) array[core][4] = 1; //MULv_sD_o�̏I��=>OUT_h���s��
    }
    return array;
  }

  //IN���ߏ��ԃt���O�ύX
  public static int[][] cIFlag(int cMax, int d, int core, int finOpeType, int finOpeNum, int[][] array){
    int Threshold = cMax/d;
    if(finOpeType == 0 && finOpeNum ==0){ //IN_x���߂̏I��
      if(core < Threshold) array[core+Threshold][0] = 1; //IN_���̏I��=>�����R�A��IN_x���s��
      else if(core >= Threshold && core != cMax-1)array[core-Threshold+1][0] = 1; //IN_���̏I��=>�����R�A��IN_x���s��
    }
    else if(finOpeType == 1){ //OUT���߂̏I��
      if(finOpeNum == 0 && core < Threshold) array[core+Threshold][1] = 1; //OUT_i���߂̏I��=>�����R�A��INADD_i���s��
      else if(finOpeNum == 0 && core >= Threshold) array[core-Threshold][1] = 1; //OUT_i���߂̏I��=>�����R�A��INADD_i���s��
      else if(finOpeNum == 1 && core < Threshold) array[core+Threshold][2] = 1; //OUT_s���߂̏I��=>�����R�A��INADD_s���s��
      else if(finOpeNum == 1 && core >= Threshold) array[core-Threshold][2] = 1; //OUT_s���߂̏I��=>�����R�A��INADD_s���s��
      else if(finOpeNum == 2 && core < Threshold) array[core+Threshold][3] = 1; //OUT_f���߂̏I��=>�����R�A��INADD_f���s��
      else if(finOpeNum == 2 && core >= Threshold) array[core-Threshold][3] = 1; //OUT_f���߂̏I��=>�����R�A��INADD_f���s��
    }
    if(finOpeType == 1 && finOpeNum == 4 && core == 2) array[0][0] = 1;//core2��OUT���߂��I��=>core0��IN_x���s�\��
    return array;
  }
}
