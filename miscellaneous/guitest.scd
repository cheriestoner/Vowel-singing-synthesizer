(
Window.closeAll;
Gui_main.new;
)

(
Window.closeAll;
STD_main.new;
)

(
FileDialog({ |path|
				postln("Selected file:" + path);
				postln("File type is:" + File.type(path));
				~textPath = path;
			},
			fileMode: 0,
			stripResult: true,
			path: "D:/VowelSynthesizer/hifi-voice" );
)

(
var filename = "C:/Users/79476/settings_22012022_1817.csv";
var filereader;


~textPath.postln;


filereader = CSVFileReader.read(~textPath, true, true).postln;
filereader[18].postln;
)

1.02324.round(0.1**3)


{10*RLPF.ar(WhiteNoise.ar(0.02), 4.0, 0.2)}.play

{SinOsc.ar(6.0, 0, 0.03, 1)}.play

(
b = SynthDef(\flutter_test, {
	arg inBus=0, outBus=2,
	ff=2.2, ampf=0.02, rqf=0.2;
	var signal, flutter;

	// signal = SoundIn.ar(inBus, 1);  // get the signal from inBus
	flutter = RLPF.ar(WhiteNoise.ar(ampf), ff, rqf);
	// flutter = 1;
	// signal = flutter*signal;
	signal = flutter*SinOsc.ar(220, 0, 1.0);
	Out.ar(outBus, signal);
};
).add;

)

a = Synth.new(\flutter_test);

(
SynthDef(\Voxsource, {
	arg outBus=0,
	note=45, invQ=0.1, scale=1.4, mul=10.0,
	fv=6.0/*5..8*/, ampv=0.03,
	ff=1.1/*4..6*/, ampf=0.02, rqf=0.1;
	var source, vibrato, flutter;
	flutter = RLPF.ar(WhiteNoise.ar(ampf), ff, rqf);
	// flutter=1.0;
	vibrato = SinOsc.ar(fv, 0, ampv, 1);
	source = ApexSource01.ar(
		flutter*vibrato*note.midicps,
		invQ:invQ, scale:scale, mult:mul);
	Out.ar(outBus, source);
};
).add
)

Synth.new(\Voxsource)