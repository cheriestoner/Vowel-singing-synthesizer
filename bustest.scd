(
SynthDef(\s, {
	arg f0=110.0, volume=0.5, outBus = 0;
	var source;

	source = Blip.ar(SinOsc.kr(6.5, 0, 0.01, f0), f0, mul:volume);
	Out.ar(outBus, source);
}
).send(Server.default);


SynthDef(\test, {
	arg inBus = 0, outBus = 0;
	var signal;

	signal = In.ar(inBus, 2);
	Out.ar(outBus, signal);
};
).send(Server.default);
)

x = Synth("s", [\outBus, 1], addAction: \addToHead);
Synth("test", [\outBus, 0, \inBus, 1], addAction: \addToHead);


(
SynthDef(\s, {
	arg f0=110.0, volume=0.5, outBus = 0;
	var source;

	source = Blip.ar(SinOsc.kr(6.5, 0, 0.01, f0), f0, mul:volume);
	Out.ar(outBus, source ! 2);
}
).play;
)

s = In.ar(0, 2);
Out.ar(0, s);