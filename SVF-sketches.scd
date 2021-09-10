
(
{ a = Blip.ar (110, mul: 1);
  b = RLPF.ar(a, MouseX.kr(50, 500), 0.1);
  RLPF.ar(b, MouseY.kr(200, 2500), 0.1);
}.play;
)


s.boot;

(
z = SynthDef(\svf_test, { arg low=0.1, band=0.0, high=0.0, notch=0.0, peak=0.0;
    var source, sigF1, sigF2, sigF3;
	source = Blip.ar(SinOsc.kr(6.5, 0, 0.01, 110), 200);

    sigF1 = SVF.ar(source,
        MouseX.kr(150,1500),    // cutoff
        0.6,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    sigF2 = SVF.ar(sigF1,
        MouseY.kr(300,2800),    // cutoff
        0.8,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    sigF3 = SVF.ar(sigF2,
        2400,    // cutoff
        0.8,           // q
        low, band, high, notch, peak, mul: 4);        // levels
    Out.ar(0, sigF3 ! 2);
}).play(addAction: \addToTail);
)

z.set(\low, 0.1);
z.set(\band, 0.1);
z.set(\high, 0.0);
z.set(\notch, 0.0);
z.set(\peak, 0.0);

// a small gui (example requires crucial lib)
(
k= [];
[\low, \band, \high, \notch, \peak].do({ arg j;
    k = k.add(KnobEditor.new(0.0, \amp).action_({ arg v; z.set(j, v.value) }) )
 });
)

(
Sheet({ arg lay;
    [\low, \band, \high, \notch, \peak].do({ arg j; CXLabel.new(lay, j, minWidth: 44) });
    lay.startRow;
    k.do(_.gui(lay));
}, "SVF")
)

z.free

(
Patch({ arg in, ffreq=1200.0, res=0.5, low=0.25, band=0.0, high=0.0, notch=0.0, peak=0.0;

    SVF.ar(
        LFSaw.ar(LFSaw.kr(2).range(110,55), 0.0, 0.1),
        ffreq, res, low, band, high, notch, peak);

}).gui
)

(
Patch({ arg low=0.25, band=0.0, high=0.0, notch=0.0, peak=0.0;
    var sig, freq;
    freq = MouseX.kr(20,12000,\exponential);
    sig = SVF.ar(
        LFSaw.ar([freq * 0.99, freq * 1.01], 0, 0.1) ! 2,
        SinOsc.kr(SinOsc.kr(0.1),1.5pi,1550,1800),
        MouseY.kr(1,0,\linear),
        low, band, high, notch, peak);
     (CombN.ar(sig, 0.4, [0.4,0.35],2) * 0.4) + (sig * 0.5);
}).gui
)