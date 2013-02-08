/* Produced by CVXGEN, 2013-02-08 14:41:12 -0500.  */
/* CVXGEN is Copyright (C) 2006-2012 Jacob Mattingley, jem@cvxgen.com. */
/* The code in this file is Copyright (C) 2006-2012 Jacob Mattingley. */
/* CVXGEN, or solvers produced by CVXGEN, cannot be used for commercial */
/* applications without prior written permission from Jacob Mattingley. */

/* Filename: testsolver.c. */
/* Description: Basic test harness for solver.c. */
#include "solver.h"
Vars vars;
Params params;
Workspace work;
Settings settings;
#define NUMTESTS 0
int main(int argc, char **argv) {
  int num_iters;
#if (NUMTESTS > 0)
  int i;
  double time;
  double time_per;
#endif
  set_defaults();
  setup_indexing();
  load_default_data();
  /* Solve problem instance for the record. */
  settings.verbose = 1;
  num_iters = solve();
#ifndef ZERO_LIBRARY_MODE
#if (NUMTESTS > 0)
  /* Now solve multiple problem instances for timing purposes. */
  settings.verbose = 0;
  tic();
  for (i = 0; i < NUMTESTS; i++) {
    solve();
  }
  time = tocq();
  printf("Timed %d solves over %.3f seconds.\n", NUMTESTS, time);
  time_per = time / NUMTESTS;
  if (time_per > 1) {
    printf("Actual time taken per solve: %.3g s.\n", time_per);
  } else if (time_per > 1e-3) {
    printf("Actual time taken per solve: %.3g ms.\n", 1e3*time_per);
  } else {
    printf("Actual time taken per solve: %.3g us.\n", 1e6*time_per);
  }
#endif
#endif
  return 0;
}
void load_default_data(void) {
  params.A[0] = 0.20319161029830202;
  params.A[1] = 0.8325912904724193;
  params.A[2] = -0.8363810443482227;
  params.A[3] = 0.04331042079065206;
  params.A[4] = 1.5717878173906188;
  params.A[5] = 1.5851723557337523;
  params.A[6] = -1.497658758144655;
  params.A[7] = -1.171028487447253;
  params.A[8] = -1.7941311867966805;
  params.A[9] = -0.23676062539745413;
  params.A[10] = -1.8804951564857322;
  params.A[11] = -0.17266710242115568;
  params.A[12] = 0.596576190459043;
  params.A[13] = -0.8860508694080989;
  params.A[14] = 0.7050196079205251;
  params.A[15] = 0.3634512696654033;
  params.A[16] = -1.9040724704913385;
  params.A[17] = 0.23541635196352795;
  params.A[18] = -0.9629902123701384;
  params.A[19] = -0.3395952119597214;
  params.A[20] = -0.865899672914725;
  params.A[21] = 0.7725516732519853;
  params.A[22] = -0.23818512931704205;
  params.A[23] = -1.372529046100147;
  params.A[24] = 0.17859607212737894;
  params.A[25] = 1.1212590580454682;
  params.A[26] = -0.774545870495281;
  params.A[27] = -1.1121684642712744;
  params.A[28] = -0.44811496977740495;
  params.A[29] = 1.7455345994417217;
  params.A[30] = 1.9039816898917352;
  params.A[31] = 0.6895347036512547;
  params.A[32] = 1.6113364341535923;
  params.A[33] = 1.383003485172717;
  params.A[34] = -0.48802383468444344;
  params.A[35] = -1.631131964513103;
  params.A[36] = 0.6136436100941447;
  params.A[37] = 0.2313630495538037;
  params.A[38] = -0.5537409477496875;
  params.A[39] = -1.0997819806406723;
  params.A[40] = -0.3739203344950055;
  params.A[41] = -0.12423900520332376;
  params.A[42] = -0.923057686995755;
  params.A[43] = -0.8328289030982696;
  params.A[44] = -0.16925440270808823;
  params.A[45] = 1.442135651787706;
  params.A[46] = 0.34501161787128565;
  params.A[47] = -0.8660485502711608;
  params.A[48] = -0.8880899735055947;
  params.A[49] = -0.1815116979122129;
  params.A[50] = -1.17835862158005;
  params.A[51] = -1.1944851558277074;
  params.A[52] = 0.05614023926976763;
  params.A[53] = -1.6510825248767813;
  params.A[54] = -0.06565787059365391;
  params.A[55] = -0.5512951504486665;
  params.A[56] = 0.8307464872626844;
  params.A[57] = 0.9869848924080182;
  params.A[58] = 0.7643716874230573;
  params.A[59] = 0.7567216550196565;
  params.A[60] = -0.5055995034042868;
  params.A[61] = 0.6725392189410702;
  params.A[62] = -0.6406053441727284;
  params.A[63] = 0.29117547947550015;
  params.A[64] = -0.6967713677405021;
  params.A[65] = -0.21941980294587182;
  params.A[66] = -1.753884276680243;
  params.A[67] = -1.0292983112626475;
  params.A[68] = 1.8864104246942706;
  params.A[69] = -1.077663182579704;
  params.A[70] = 0.7659100437893209;
  params.A[71] = 0.6019074328549583;
  params.A[72] = 0.8957565577499285;
  params.A[73] = -0.09964555746227477;
  params.A[74] = 0.38665509840745127;
  params.A[75] = -1.7321223042686946;
  params.A[76] = -1.7097514487110663;
  params.A[77] = -1.2040958948116867;
  params.A[78] = -1.3925560119658358;
  params.A[79] = -1.5995826216742213;
  params.A[80] = -1.4828245415645833;
  params.A[81] = 0.21311092723061398;
  params.A[82] = -1.248740700304487;
  params.A[83] = 1.808404972124833;
  params.A[84] = 0.7264471152297065;
  params.A[85] = 0.16407869343908477;
  params.A[86] = 0.8287224032315907;
  params.A[87] = -0.9444533161899464;
  params.A[88] = 1.7069027370149112;
  params.A[89] = 1.3567722311998827;
  params.A[90] = 0.9052779937121489;
  params.A[91] = -0.07904017565835986;
  params.A[92] = 1.3684127435065871;
  params.A[93] = 0.979009293697437;
  params.A[94] = 0.6413036255984501;
  params.A[95] = 1.6559010680237511;
  params.A[96] = 0.5346622551502991;
  params.A[97] = -0.5362376605895625;
  params.A[98] = 0.2113782926017822;
  params.A[99] = -1.2144776931994525;
  params.A[100] = -1.2317108144255875;
  params.A[101] = 0.9026784957312834;
  params.A[102] = 1.1397468137245244;
  params.A[103] = 1.8883934547350631;
  params.A[104] = 1.4038856681660068;
  params.A[105] = 0.17437730638329096;
  params.A[106] = -1.6408365219077408;
  params.A[107] = -0.04450702153554875;
  params.A[108] = 1.7117453902485025;
  params.A[109] = 1.1504727980139053;
  params.A[110] = -0.05962309578364744;
  params.A[111] = -0.1788825540764547;
  params.A[112] = -1.1280569263625857;
  params.A[113] = -1.2911464767927057;
  params.A[114] = -1.7055053231225696;
  params.A[115] = 1.56957275034837;
  params.A[116] = 0.5607064675962357;
  params.A[117] = -1.4266707301147146;
  params.A[118] = -0.3434923211351708;
  params.A[119] = -1.8035643024085055;
  params.A[120] = -1.1625066019105454;
  params.A[121] = 0.9228324965161532;
  params.A[122] = 0.6044910817663975;
  params.A[123] = -0.0840868104920891;
  params.A[124] = -0.900877978017443;
  params.A[125] = 0.608892500264739;
  params.A[126] = 1.8257980452695217;
  params.A[127] = -0.25791777529922877;
  params.A[128] = -1.7194699796493191;
  params.A[129] = -1.7690740487081298;
  params.A[130] = -1.6685159248097703;
  params.A[131] = 1.8388287490128845;
  params.A[132] = 0.16304334474597537;
  params.A[133] = 1.3498497306788897;
  params.A[134] = -1.3198658230514613;
  params.A[135] = -0.9586197090843394;
  params.A[136] = 0.7679100474913709;
  params.A[137] = 1.5822813125679343;
  params.A[138] = -0.6372460621593619;
  params.A[139] = -1.741307208038867;
  params.A[140] = 1.456478677642575;
  params.A[141] = -0.8365102166820959;
  params.A[142] = 0.9643296255982503;
  params.A[143] = -1.367865381194024;
  params.A[144] = 0.7798537405635035;
  params.A[145] = 1.3656784761245926;
  params.A[146] = 0.9086083149868371;
  params.A[147] = -0.5635699005460344;
  params.A[148] = 0.9067590059607915;
  params.A[149] = -1.4421315032701587;
  params.A[150] = -0.7447235390671119;
  params.A[151] = -0.32166897326822186;
  params.A[152] = 1.5088481557772684;
  params.A[153] = -1.385039165715428;
  params.A[154] = 1.5204991609972622;
  params.A[155] = 1.1958572768832156;
  params.A[156] = 1.8864971883119228;
  params.A[157] = -0.5291880667861584;
  params.A[158] = -1.1802409243688836;
  params.A[159] = -1.037718718661604;
  params.A[160] = 1.3114512056856835;
  params.A[161] = 1.8609125943756615;
  params.A[162] = 0.7952399935216938;
  params.A[163] = -0.07001183290468038;
  params.A[164] = -0.8518009412754686;
  params.A[165] = 1.3347515373726386;
  params.A[166] = 1.4887180335977037;
  params.A[167] = -1.6314736327976336;
  params.A[168] = -1.1362021159208933;
  params.A[169] = 1.327044361831466;
  params.A[170] = 1.3932155883179842;
  params.A[171] = -0.7413880049440107;
  params.A[172] = -0.8828216126125747;
  params.A[173] = -0.27673991192616;
  params.A[174] = 0.15778600105866714;
  params.A[175] = -1.6177327399735457;
  params.A[176] = 1.3476485548544606;
  params.A[177] = 0.13893948140528378;
  params.A[178] = 1.0998712601636944;
  params.A[179] = -1.0766549376946926;
  params.A[180] = 1.8611734044254629;
  params.A[181] = 1.0041092292735172;
  params.A[182] = -0.6276245424321543;
  params.A[183] = 1.794110587839819;
  params.A[184] = 0.8020471158650913;
  params.A[185] = 1.362244341944948;
  params.A[186] = -1.8180107765765245;
  params.A[187] = -1.7774338357932473;
  params.A[188] = 0.9709490941985153;
  params.A[189] = -0.7812542682064318;
  params.A[190] = 0.0671374633729811;
  params.A[191] = -1.374950305314906;
  params.A[192] = 1.9118096386279388;
  params.A[193] = 0.011004190697677885;
  params.A[194] = 1.3160043138989015;
  params.A[195] = -1.7038488148800144;
  params.A[196] = -0.08433819112864738;
  params.A[197] = -1.7508820783768964;
  params.A[198] = 1.536965724350949;
  params.A[199] = -0.21675928514816478;
  params.A[200] = -1.725800326952653;
  params.A[201] = -1.6940148707361717;
  params.A[202] = 0.15517063201268;
  params.A[203] = -1.697734381979077;
  params.A[204] = -1.264910727950229;
  params.A[205] = -0.2545716633339441;
  params.A[206] = -0.008868675926170244;
  params.A[207] = 0.3332476609670296;
  params.A[208] = 0.48205072561962936;
  params.A[209] = -0.5087540014293261;
  params.A[210] = 0.4749463319223195;
  params.A[211] = -1.371021366459455;
  params.A[212] = -0.8979660982652256;
  params.A[213] = 1.194873082385242;
  params.A[214] = -1.3876427970939353;
  params.A[215] = -1.106708108457053;
  params.A[216] = -1.0280872812241797;
  params.A[217] = -0.08197078070773234;
  params.A[218] = -1.9970179118324083;
  params.A[219] = -1.878754557910134;
  params.A[220] = -0.15380739340877803;
  params.A[221] = -1.349917260533923;
  params.A[222] = 0.7180072150931407;
  params.A[223] = 1.1808183487065538;
  params.A[224] = 0.31265343495084075;
  params.A[225] = 0.7790599086928229;
  params.A[226] = -0.4361679370644853;
  params.A[227] = -1.8148151880282066;
  params.A[228] = -0.24231386948140266;
  params.A[229] = -0.5120787511622411;
  params.A[230] = 0.3880129688013203;
  params.A[231] = -1.4631273212038676;
  params.A[232] = -1.0891484131126563;
  params.A[233] = 1.2591296661091191;
  params.A[234] = -0.9426978934391474;
  params.A[235] = -0.358719180371347;
  params.A[236] = 1.7438887059831263;
  params.A[237] = -0.8977901479165817;
  params.A[238] = -1.4188401645857445;
  params.A[239] = 0.8080805173258092;
  params.A[240] = 0.2682662017650985;
  params.A[241] = 0.44637534218638786;
  params.A[242] = -1.8318765960257055;
  params.A[243] = -0.3309324209710929;
  params.A[244] = -1.9829342633313622;
  params.A[245] = -1.013858124556442;
  params.A[246] = 0.8242247343360254;
  params.A[247] = -1.753837136317201;
  params.A[248] = -0.8212260055868805;
  params.A[249] = 1.9524510112487126;
  params.A[250] = 1.884888920907902;
  params.A[251] = -0.0726144452811801;
  params.A[252] = 0.9427735461129836;
  params.A[253] = 0.5306230967445558;
  params.A[254] = -0.1372277142250531;
  params.A[255] = 1.4282657305652786;
  params.A[256] = -1.309926991335284;
  params.A[257] = 1.3137276889764422;
  params.A[258] = -1.8317219061667278;
  params.A[259] = 1.4678147672511939;
  params.A[260] = 0.703986349872991;
  params.A[261] = -0.2163435603565258;
  params.A[262] = 0.6862809905371079;
  params.A[263] = -0.15852598444303245;
  params.A[264] = 1.1200128895143409;
  params.A[265] = -1.5462236645435308;
  params.A[266] = 0.0326297153944215;
  params.A[267] = 1.4859581597754916;
  params.A[268] = 1.71011710324809;
  params.A[269] = -1.1186546738067493;
  params.A[270] = -0.9922787897815244;
  params.A[271] = 1.6160498864359547;
  params.A[272] = -0.6179306451394861;
  params.A[273] = -1.7725097038051376;
  params.A[274] = 0.8595466884481313;
  params.A[275] = -0.3423245633865686;
  params.A[276] = 0.9412967499805762;
  params.A[277] = -0.09163346622652258;
  params.A[278] = 0.002262217745727657;
  params.A[279] = -0.3297523583656421;
  params.A[280] = -0.8380604158593941;
  params.A[281] = 1.6028434695494038;
  params.A[282] = 0.675150311940429;
  params.A[283] = 1.1553293733718686;
  params.A[284] = 1.5829581243724693;
  params.A[285] = -0.9992442304425597;
  params.A[286] = 1.6792824558896897;
  params.A[287] = 1.4504203490342324;
  params.A[288] = 0.02434104849994556;
  params.A[289] = 0.27160869657612263;
  params.A[290] = -1.5402710478528858;
  params.A[291] = 1.0484633622310744;
  params.A[292] = -1.3070999712627054;
  params.A[293] = 0.13534416402363814;
  params.A[294] = -1.4942507790851232;
  params.A[295] = -1.708331625671371;
  params.A[296] = 0.436109775042258;
  params.A[297] = -0.03518748153727991;
  params.A[298] = 0.6992397389570906;
  params.A[299] = 1.1634167322171374;
  params.A[300] = 1.9307499705822648;
  params.A[301] = -1.6636772756932747;
  params.A[302] = 0.5248484497343218;
  params.A[303] = 0.30789958152579144;
  params.A[304] = 0.602568707166812;
  params.A[305] = 0.17271781925751872;
  params.A[306] = 0.2294695501208066;
  params.A[307] = 1.4742185345619543;
  params.A[308] = -0.1919535345136989;
  params.A[309] = 0.13990231452144553;
  params.A[310] = 0.7638548150610602;
  params.A[311] = -1.6420200344195646;
  params.A[312] = -0.27229872445076087;
  params.A[313] = -1.5914631171820468;
  params.A[314] = -1.4487604283558668;
  params.A[315] = -1.991497766136364;
  params.A[316] = -1.1611742553535152;
  params.A[317] = -1.133450950247063;
  params.A[318] = 0.06497792493777155;
  params.A[319] = 0.28083295396097263;
  params.A[320] = 1.2958447220129887;
  params.A[321] = -0.05315524470737154;
  params.A[322] = 1.5658183956871667;
  params.A[323] = -0.41975684089933685;
  params.A[324] = 0.97844578833777;
  params.A[325] = 0.2110290496695293;
  params.A[326] = 0.4953003430893044;
  params.A[327] = -0.9184320124667495;
  params.A[328] = 1.750380031759156;
  params.A[329] = 1.0786188614315915;
  params.A[330] = -1.4176198837203735;
  params.A[331] = 0.149737479778294;
  params.A[332] = 1.9831452222223418;
  params.A[333] = -1.8037746699794734;
  params.A[334] = -0.7887206483295461;
  params.A[335] = 0.9632534854086652;
  params.A[336] = -1.8425542093895406;
  params.A[337] = 0.986684363969033;
  params.A[338] = 0.2936851199350441;
  params.A[339] = 0.9268227022482662;
  params.A[340] = 0.20333038350653299;
  params.A[341] = 1.7576139132046351;
  params.A[342] = -0.614393188398918;
  params.A[343] = 0.297877839744912;
  params.A[344] = -1.796880083990895;
  params.A[345] = 0.21373133661742738;
  params.A[346] = -0.32242822540825156;
  params.A[347] = 1.9326471511608059;
  params.A[348] = 1.7824292753481785;
  params.A[349] = -1.4468823405675986;
  params.A[350] = -1.8335374338761512;
  params.A[351] = -1.5172997317243713;
  params.A[352] = -1.229012129120719;
  params.A[353] = 0.9046719772422094;
  params.A[354] = 0.17591181415489432;
  params.A[355] = 0.13970133814112584;
  params.A[356] = -0.14185208214985234;
  params.A[357] = -1.9732231264739348;
  params.A[358] = -0.4301123458221334;
  params.A[359] = 1.9957537650387742;
  params.A[360] = 1.2811648216477893;
  params.A[361] = 0.2914428437588219;
  params.A[362] = -1.214148157218884;
  params.A[363] = 1.6818776980374155;
  params.A[364] = -0.30341101038214635;
  params.A[365] = 0.47730909231793106;
  params.A[366] = -1.187569373035299;
  params.A[367] = -0.6877370247915531;
  params.A[368] = -0.6201861482616171;
  params.A[369] = -0.4209925183921568;
  params.A[370] = -1.9110724537712471;
  params.A[371] = 0.6413882087807936;
  params.A[372] = -1.3200399280087032;
  params.A[373] = 0.41320105301312626;
  params.A[374] = 0.4783213861392275;
  params.A[375] = 0.7916189857293743;
  params.A[376] = -0.8322752558146558;
  params.A[377] = -0.8318720537426154;
  params.A[378] = 1.0221179076113445;
  params.A[379] = -0.4471032189262627;
  params.A[380] = -1.3901469561676985;
  params.A[381] = 1.6210596051208572;
  params.A[382] = -1.9476687601912737;
  params.A[383] = 1.5459376306231292;
  params.W[0] = -0.830972896191656;
  params.W[1] = -0.47269983955176276;
  params.W[2] = 1.913620609584223;
  params.W[3] = -0.25329703423935124;
  params.W[4] = 0.8635279149674653;
  params.W[5] = -0.35046893227111564;
  params.C[0] = 1.9135358121693091;
  params.C[1] = 1.7194904992103375;
  params.C[2] = 1.4806917884353878;
  params.C[3] = 1.08422164898412;
  params.C[4] = 1.3636338678287099;
  params.C[5] = 1.49060670234726;
  params.epsilon[0] = 0.5677283669027675;
  params.B[0] = 0.13856203767859343;
  params.B[1] = -1.1613957272733684;
  params.B[2] = -0.022681697832835024;
  params.B[3] = 0.11202078062843634;
  params.B[4] = 0.6934385624164641;
  params.B[5] = 0.9814633803279791;
  params.B[6] = 0.9198949681022897;
  params.B[7] = -0.3035363988458051;
  params.B[8] = -0.1761906755724203;
  params.B[9] = 1.4940284058791686;
  params.B[10] = -0.5488483097174393;
  params.B[11] = 0.9521313238305416;
  params.B[12] = 1.9762689267600413;
  params.B[13] = 1.6992335341478482;
  params.B[14] = 0.1969474711697119;
  params.B[15] = -0.7795544525014559;
  params.B[16] = 0.4892505434034007;
  params.B[17] = 0.7372066729248594;
  params.B[18] = 0.10784901966517557;
  params.B[19] = -0.6340934767066218;
  params.B[20] = -0.17829371464242083;
  params.B[21] = -1.6728370279392784;
  params.B[22] = -0.8348711800042916;
  params.B[23] = -1.4204129800590897;
  params.B[24] = 0.6659229232859376;
  params.B[25] = 1.8369365661533168;
  params.B[26] = -1.371061267737546;
  params.B[27] = -1.8868237125934915;
  params.B[28] = 0.9654286768651104;
  params.B[29] = -0.5833420409292005;
  params.B[30] = 0.02386510653728502;
  params.B[31] = -1.7558076992858345;
  params.B[32] = -1.2889402130475411;
  params.B[33] = 0.7820251677632606;
  params.B[34] = 0.4208424784688227;
  params.B[35] = 1.4136448896755982;
  params.B[36] = 1.8516928541530757;
  params.B[37] = -0.5615396035790421;
  params.B[38] = 0.4809940266433177;
  params.B[39] = -0.20929035114697303;
  params.B[40] = 0.022387850798402553;
  params.B[41] = -0.43399296564115764;
  params.B[42] = 1.9095769077945013;
  params.B[43] = 0.4945512698336847;
  params.B[44] = -1.4324582900293557;
  params.B[45] = 0.790913765746676;
  params.B[46] = 1.8630250293383734;
  params.B[47] = 1.5793975466121069;
  params.B[48] = 0.2320163334712646;
  params.B[49] = -1.9411408650055968;
  params.B[50] = 1.2221853270725478;
  params.B[51] = 1.7274453600045607;
  params.B[52] = 0.9357159281665783;
  params.B[53] = -0.2841874908331623;
  params.B[54] = -0.4766355664552626;
  params.B[55] = 0.9784190546201912;
  params.B[56] = -1.5685956114005477;
  params.B[57] = 1.1387833891036;
  params.B[58] = -0.004779126480003892;
  params.B[59] = -1.7195239474925414;
  params.B[60] = 1.2921808565147272;
  params.B[61] = -0.43317009071966606;
  params.B[62] = -1.572940257279357;
  params.B[63] = -1.3048062231674988;
  params.B[64] = 1.4377304631579175;
  params.B[65] = -1.3090328020145874;
  params.B[66] = 1.1370018620707785;
  params.B[67] = 1.2164644012668289;
  params.B[68] = -1.6539274174499985;
  params.B[69] = -0.25845368809725544;
  params.B[70] = 1.1486358936399745;
  params.B[71] = -0.03975647517318137;
  params.B[72] = 1.4640632749164326;
  params.B[73] = -0.48111499989733186;
  params.B[74] = 0.5132576752843594;
  params.B[75] = -1.1459189400462249;
  params.B[76] = 1.3690255364554855;
  params.B[77] = 1.3574291456003253;
  params.B[78] = 0.26333733823037253;
  params.B[79] = -0.7076462135286032;
  params.B[80] = -0.6097272363453645;
  params.B[81] = 0.37873096815108465;
  params.B[82] = -1.4863636934585411;
  params.B[83] = 0.04189135833804869;
  params.B[84] = -0.8182949160834703;
  params.B[85] = -0.6336865828985854;
  params.B[86] = -0.7126437991119396;
  params.B[87] = 1.3381487344587226;
  params.B[88] = -1.2979975504895949;
  params.B[89] = -1.0542097271412714;
  params.B[90] = -1.3421003125955435;
  params.B[91] = -1.9395969070507038;
  params.B[92] = -0.29758108058547306;
  params.B[93] = 1.3757899684264032;
  params.B[94] = 1.6109970296148042;
  params.B[95] = -0.050537352418498216;
  params.B[96] = -0.3144945653528741;
  params.B[97] = 1.4726689240031474;
  params.B[98] = 0.11397910876468265;
  params.B[99] = 0.19466869962815858;
  params.B[100] = 0.5972476722406035;
  params.B[101] = -1.6815490772221828;
  params.B[102] = 1.3540223072599735;
  params.B[103] = -1.577027832358222;
  params.B[104] = 0.12928618615237353;
  params.B[105] = 1.704038169667271;
  params.B[106] = 0.19482725189070793;
  params.B[107] = -0.6311686254597215;
  params.B[108] = 0.9065234706582928;
  params.B[109] = 1.604058201281767;
  params.B[110] = 0.4649414640474294;
  params.B[111] = -1.7764554290993346;
  params.B[112] = 1.5152343936830337;
  params.B[113] = -1.9280901945449935;
  params.B[114] = 0.7129569482366098;
  params.B[115] = 1.6001840923928201;
  params.B[116] = -1.3702177446733126;
  params.B[117] = 0.11266051920028186;
  params.B[118] = 0.8202183589903962;
  params.B[119] = -1.297953481011172;
  params.B[120] = -1.0192096617939002;
  params.B[121] = -1.7337200441949867;
  params.B[122] = -1.3639899659742465;
  params.B[123] = -1.5273517222086332;
  params.B[124] = -0.8374302703303731;
  params.B[125] = 1.00229367551592;
  params.B[126] = 0.7747378843920099;
  params.B[127] = 1.0504096866871468;
  params.B[128] = 0.638655773812761;
  params.B[129] = 1.176936790033046;
  params.B[130] = -1.4041747524796162;
  params.B[131] = 0.21725437512222667;
  params.B[132] = -1.9141609882936188;
  params.B[133] = -0.03334441105363828;
  params.B[134] = 1.3736673884387467;
  params.B[135] = -0.11085150689269163;
  params.B[136] = -0.8176560931958075;
  params.B[137] = -0.9013799953302866;
  params.B[138] = -0.42583422050124753;
  params.B[139] = 1.6552920005330618;
  params.B[140] = 1.8971842560697287;
  params.B[141] = 0.9935321777966784;
  params.B[142] = 1.9500402929402196;
  params.B[143] = 1.0489535977170181;
  params.B[144] = -0.8630392743714372;
  params.B[145] = -0.25967183338596733;
  params.B[146] = 0.8925966402843359;
  params.B[147] = 0.8373600738876834;
  params.B[148] = 0.7125001994938436;
  params.B[149] = -0.048447588572545275;
  params.B[150] = -1.4274714856193604;
  params.B[151] = 1.8385542904833923;
  params.B[152] = -1.1195070325474288;
  params.B[153] = 1.9175373793884956;
  params.B[154] = -1.49030500627704;
  params.B[155] = 1.9213425364706396;
  params.B[156] = -0.49553546476315047;
  params.B[157] = 1.2437464435895134;
  params.B[158] = -1.970831509470568;
  params.B[159] = -0.219996830259797;
  params.B[160] = -1.0042329091607591;
  params.B[161] = 0.7781008085794774;
  params.B[162] = 0.65210699599452;
  params.B[163] = -0.152326999732443;
  params.B[164] = 0.8265434509993406;
  params.B[165] = 1.9130464561754126;
  params.B[166] = -1.6270096836882288;
  params.B[167] = 0.2507042290048189;
  params.B[168] = 0.7038441998600256;
  params.B[169] = 0.5328743207925606;
  params.B[170] = -0.9509907719589208;
  params.B[171] = 1.499815178589135;
  params.B[172] = -1.0178753663037017;
  params.B[173] = 1.3798461831617561;
  params.B[174] = -0.11708553759234386;
  params.B[175] = -1.4276299186218124;
  params.B[176] = 1.296518419303864;
  params.B[177] = -1.6872707956138546;
  params.B[178] = 1.1799585157870145;
  params.B[179] = 0.4000488706320535;
  params.B[180] = 1.506638004200894;
  params.B[181] = 1.2128180682740366;
  params.B[182] = -0.39211699471717854;
  params.B[183] = -1.4592313874139302;
  params.B[184] = -0.9352340128154211;
  params.B[185] = -1.994709862977336;
  params.B[186] = 0.6136129920637026;
  params.B[187] = -1.6579503948780245;
  params.B[188] = -1.2828456921062488;
  params.B[189] = -1.0200938896697522;
  params.B[190] = -0.3755900704115436;
  params.B[191] = 0.747199791836243;
  params.B[192] = -0.22212974213441683;
  params.B[193] = 0.015082263441096089;
  params.B[194] = -1.6271688108937168;
  params.B[195] = -0.6472903955867526;
  params.B[196] = -1.1733258209627806;
  params.B[197] = 0.9565501943340924;
  params.B[198] = -1.929389541307601;
  params.B[199] = 0.4671837668673531;
  params.B[200] = 0.7915477026785647;
  params.B[201] = 0.018572068486599758;
  params.B[202] = -1.8220899973808726;
  params.B[203] = -0.995629851336445;
  params.B[204] = -1.0486975119711213;
  params.B[205] = -0.9289312699596386;
  params.B[206] = -0.9472402942019333;
  params.B[207] = 1.8908619466142156;
  params.B[208] = 1.164645007668001;
  params.B[209] = 1.5636429264767182;
  params.B[210] = 0.8540115800503387;
  params.B[211] = -0.6133530465568309;
  params.B[212] = 1.7674136894457204;
  params.B[213] = -0.06217940181271242;
  params.B[214] = -1.2582602406204213;
  params.B[215] = 0.9179968784775836;
  params.B[216] = -0.9627796203753647;
  params.B[217] = 1.2911416493727805;
  params.B[218] = 0.9619156621267284;
  params.B[219] = -0.8391987363014124;
  params.B[220] = -0.16142857857315818;
  params.B[221] = 0.8603892868304936;
  params.B[222] = 0.672061858055037;
  params.B[223] = 0.10631385676272265;
  params.B[224] = -1.1434283104802896;
  params.B[225] = -0.7024280087663541;
  params.B[226] = 0.7791723379458899;
  params.B[227] = -0.17206766925671246;
  params.B[228] = 0.8714406054415362;
  params.B[229] = 0.7364640800101268;
  params.B[230] = -0.577393318625969;
  params.B[231] = -1.603607371381821;
  params.B[232] = 0.7231454736647596;
  params.B[233] = -0.5776666119800344;
  params.B[234] = 0.25985922282642804;
  params.B[235] = -1.500019293846674;
  params.B[236] = -1.41591503759888;
  params.B[237] = -0.30464385789747794;
  params.B[238] = 0.677515340905404;
  params.B[239] = -1.5301412809058377;
  params.B[240] = 1.097788736551506;
  params.B[241] = 1.4054563154505293;
  params.B[242] = 0.6904915185274869;
  params.B[243] = 0.9984361169236493;
  params.B[244] = -1.0460788838474921;
  params.B[245] = -1.5989319614177124;
  params.B[246] = -0.6834813660758638;
  params.B[247] = -1.4978328637140224;
  params.B[248] = -0.3340404173113156;
  params.B[249] = 1.044497402438696;
  params.B[250] = -0.875611719278079;
  params.B[251] = 1.4233779191761733;
  params.B[252] = -0.1880612910960302;
  params.B[253] = -1.3523791242997114;
  params.B[254] = 0.5691200673315562;
  params.B[255] = -0.24590364206081272;
  params.fmin[0] = -0.6790819241936314;
  params.fmin[1] = 0.06554105230580287;
  params.fmin[2] = 1.9642897275976492;
  params.fmin[3] = 1.0075323744403706;
}
