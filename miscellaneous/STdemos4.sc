/* Class definitions for Sten Ternström's SC demos */
/* TODO general
   Resize scope/spectrum
   Change maxfreq of spectrum display
*/

// Robust storage of global constants
STD_c{
	*bDebug{ ^true }
	*nBufSize{ ^1024 }
	*hRow1{ ^285 }
} /* STD_c */


STD_main{
	var modules, scope, spectrum;

	// this is a normal constructor method
	*new { /* arg arga,argb,argc; */ // * indicates this is a class method
		^super.new.init( /* arga,argb,argc */ )
	}

	init {
		modules = [["Audio in", "F2/F1", "Precedence", "Comb filter"],
			       ["Stop", "Stop", "Stop", "Stop"],
			       [STD_audioIn, STD_f1f2, STD_precedence, STD_combfilter]];
		Server.default.waitForBoot({
			postln("Server booted.");
			this.createPanel;
			this.compileSynthDefs;
			Server.default.sync;
		});
	}

	compileSynthDefs {
		modules[2].do {|class| if (class.notNil, { class.compileSynthDefs } )}
	}

	createPanel {
		var window, selectionView, containerView, visibleViews,
		scopeView, spectrumView, buttons, visible, reLayout;

		// Function that redoes the layout of the container view
		reLayout = {
			visibleViews = [];
			visible.do({|bool, i|
				var view;
				if(bool, {
					// generate new view, has to be done since removal of view makes it unusable again
					if (nil != modules[2][i], {
						var a = modules[2][i].new(this); a.postln;
						view = a.view;
						}, {
							view = View.new;
							view.background = Color.hsv(i / 8.0, 0.1, 1);
							view.minSize = Size.new(150, 250);
					});
					visibleViews = visibleViews.add(view);
				});
			});
			// removes all children and make them unusable
			containerView.removeAll;
			containerView.layout = HLayout.new(*visibleViews);
			containerView.refresh;
		};

		// Selection buttons
				buttons = 4.collect({|i|		// Better, do something like modules.collect()...
					var button;
					button = Button.new;
					button.states = [[modules[0][i], Color.black, Color.green],
									 [modules[1][i], Color.white, Color.red ]];
					button.action = {|v|
						visible[i] = (v.value == 1);
						reLayout.();
					}
				});


		// Record of what views are visible, same number of booleans as the number of buttons
		visible = false ! buttons.size;

		// View that contain the buttons
		selectionView = View.new;
		selectionView.layout = VLayout.new(*buttons);
		selectionView.maxSize = Size.new(200, 2000);
		selectionView.minSize = Size.new(80, 150);

		containerView = View.new;

		window = Window.new("Sten's Demos 4", Rect(100, 100, 800, 500)).front;

		scope = STD_scope.new();
		scopeView = scope.view;

		spectrum = STD_spectrum.new();
		spectrumView = spectrum.view;


		// window.layout = HLayout.new(selectionView, containerView);
		window.layout = VLayout.new(
			HLayout.new([scopeView, stretch: 2], [spectrumView, stretch: 3]),
			HLayout.new(selectionView, containerView)
		);
		window.layout.margins_(1);
		spectrum.run(false);

		window.onClose_({ // don't forget this
			spectrum.run(false);
			scope.free;
		});

	} /* STD_main.createPanel */

	trigBus {
		^scope.trigBus;
	}

} /* STD_main */

// STD_selection

STD_scope{
	/* TODO:
	*  Make the scope resizeable (it's too small now)
	*  Maybe base it on ScopeView rather than Stethoscope which has too many controls
	*  Add time axis
	*  Fix the sync problem if possible
	*/
	var <view, scopeBus, <trigBus, scopeBuf, synth;

	// this is a normal constructor method
	*new {|... args| // * indicates this is a class method
		^super.new.init(*args);
	}

	init { // arg wParent;
		view = View.new;
		// view.minSize = Size.new(270, 300);
		scopeBuf = Buffer.alloc(Server.default, STD_c.nBufSize, 2);
		scopeBus = Bus.audio(Server.default, 3);  // left, right, trig
		trigBus  = Bus.newFrom(scopeBus, 2, 1);   // for easier access to trig bus
		this.compileSynthDefs;
		this.createPanel;
	}

	compileSynthDefs {
		// Triggers a periodic (?) signal, for scoping
		// Works best with bufsize <= 1024
		SynthDef(\STDscope, {
			arg busIn = 0, busOut = 0, tau = 0.995, bufSize = 1024;
			var lBuffer, input, signal, aligned, z, phi, pulse_rate;

			lBuffer = LocalBuf(bufSize, 1);
			pulse_rate = bufSize/Server.default.sampleRate;
			// Get signal from a given bus
			input = In.ar(busIn, 1);
			// Cycle detection. Input is offset +1 to work around the fact
			// that PeakFollower first takes the absolute value of its input.
			// So signal magnitudes should be < 1 .
			signal = HPF.ar(PeakFollower.ar(LPZ2.ar(input, 1, 1), tau), 20);
			// If positive zero-crossing, output one "1"
			// and hold high for at least the duration of the buffer
			z = Trig1.ar(signal, pulse_rate);
			// Construct a phase phi for BufWr
			phi = Phasor.ar(z, 1, 0, 2*bufSize, 0).clip2(bufSize-1);
			// Start writing into lBuffer on the z trigger
			BufWr.ar(input, lBuffer, phi, 0);
			// PlayBuf seems to work
			aligned = PlayBuf.ar(1, lBuffer, rate: 1, trigger: 1, startPos: 0, loop: 1);
			ReplaceOut.ar(busOut, aligned);
		}).send(Server.default);

		// Two-signal input, triggered by the first, or by the trig channel
		// Triggers a periodic (?) signal, for scoping
		// Works best with bufsize <= 1024
		SynthDef(\STDscope2, {
			arg busIn = 0, busOut = 0, tau = 0.995, trigSelect = 0, bufSize = 1024;
			var lBuffer, input, signal, trig, aligned, z, phi, pulse_dur;

			lBuffer = LocalBuf(bufSize, 2);
			pulse_dur = bufSize/Server.default.sampleRate;
			// Get signals from given buses
			input = In.ar(busIn, 2);
			trig = Select.ar(trigSelect, [input[0], input[1], In.ar(busOut+2)]);

			// Cycle detection. Input is offset +1 to work around the fact
			// that PeakFollower first takes the absolute value of its input.
			// So signal magnitudes should be < 1 .
			signal = HPF.ar(PeakFollower.ar(LPZ2.ar(trig, 1, 1), tau), 20);
			// If positive zero-crossing, output one "1"
			// and hold high for at least twice the duration of the buffer
			z = Trig1.ar(signal, pulse_dur);
			// Construct a phase phi for BufWr
			phi = Phasor.ar(z, 1, 0, 2*bufSize, 0).clip2(bufSize);
			// phi = Phasor.ar(z, 1, 0, bufSize, 0);
			// Start writing into lBuffer on the z trigger
			BufWr.ar(input, lBuffer, phi, 0);
			// PlayBuf seems to work
			aligned = PlayBuf.ar(2, lBuffer, rate: 1, trigger: 1, startPos: 0, loop: 1);
			// ReplaceOut.ar(busOut, aligned);
			OffsetOut.ar(busOut, aligned);
		}).send(Server.default);

		SynthDef(\STDscope3, {
			arg busIn = 0, busOut = 0, tau = 0.995, bufSize = 1024;
			var lBuffer, input, signalPlus, signalMinus, aligned, z, phi, pulse_rate;

			lBuffer = LocalBuf(bufSize, 1);
			pulse_rate = bufSize/Server.default.sampleRate;
			// Get signal from a given bus
			input = In.ar(busIn, 1);
			// Cycle detection. Input is offset +1 to work around the fact
			// that PeakFollower first takes the absolute value of its input.
			// So signal magnitudes should be < 1 .
			signalPlus  = HPF.ar(PeakFollower.ar(LPZ2.ar(input, 1, 1), tau), 20);
			signalMinus = HPF.ar(PeakFollower.ar(LPZ2.ar(-1.0*input, 1, 1), tau), 20);

			// When a negative peak is followed by a positive peak, output one "1"
			// and hold high for at least the duration of the buffer
			z = Trig1.ar(SetResetFF.ar(signalPlus, signalMinus), pulse_rate);
			// Construct a phase phi for BufWr
			phi = Phasor.ar(z, 1, 0, 2*bufSize, 0).clip2(bufSize-1);
			// Start writing into lBuffer on the z trigger
			BufWr.ar(input, lBuffer, phi, 0);
			// PlayBuf seems to work
			aligned = PlayBuf.ar(1, lBuffer, rate: 1, trigger: 1, startPos: 0, loop: 1);
			ReplaceOut.ar(busOut, aligned);
		}).send(Server.default);

		// For use with ScopeView rather than Stethoscope
		// ScopeView seems to require Server.internal rather than Server.localhost,
		// but this breaks the spectrum display...
		SynthDef(\STDscope_alt, {
			arg busIn = 0, buffer, bufSize = STD_c.nBufSize, tau = 0.995;
			var input, signal, aligned, z, phi, pulse_rate;

			pulse_rate = bufSize/Server.default.sampleRate;
			// Get signal from a given bus
			input = In.ar(busIn, 1);
			// Cycle detection. Input is offset +1 to work around the fact
			// that PeakFollower first takes the absolute value of its input.
			// So signal magnitudes should be < 1 .
			signal = HPF.ar(PeakFollower.ar(LPZ2.ar(input, 1, 1), tau), 20);
			// If positive zero-crossing, output one "1"
			// and hold high for at least the duration of the buffer
			z = Trig1.ar(signal, pulse_rate);
			// Construct a phase phi for BufWr
			phi = Phasor.ar(z, 1, 0, 2*bufSize, 0).clip2(bufSize-1);
			// Start writing into lBuffer on the z trigger
			BufWr.ar(input, buffer, phi, 0);
			// PlayBuf seems to work
			aligned = PlayBuf.ar(1, buffer, rate: 1, trigger: 1, startPos: 0, loop: 1);
			ScopeOut2.ar(aligned, buffer);
		}).send(Server.default);

		Server.default.sync;
	}

	createPanel {
		var button, trigButton, scope;

		/* Here I would prefer a ScopeView rather than a Stethoscope,
		with all the unnecessary controls around it, but I haven't gotten it to work */
		scope = Stethoscope.new(Server.default, numChannels: 1, index: scopeBus.index,
			bufsize: STD_c.nBufSize, view: view);
		scope.scopeView.fill_(false);

		// scope.size_(view.bounds.width);  // This seems to have no effect

		// add a button to start and stop the displays.
		button = Button(view, 40 @ 20);
		button.states = [["Scope on", Color.black, Color.green],["Scope off", Color.white, Color.red]];
		button.action = {|view|
			if (view.value == 1) {
				synth = Synth.new(\STDscope,
					[\busOut, scopeBus, \bufSize, STD_c.nBufSize],
					addAction: \addToTail);
				scope.scopeView.waveColors = Color.green ! 2;
				trigButton.visible_(true);
			};
			if (view.value == 0) {
				synth.free;
				trigButton.visible_(false);
			};
		};

		// add a button to select the trig source
		trigButton = Button(view, 40 @ 20);
		trigButton.states = [
			["Trig on X", Color.black, Color.gray],
			["Trig on Y", Color.white, Color.gray],
			["Trig on Ext", Color.red, Color.gray]
		];
		trigButton.action = {|b| synth.notNil.if ({ this.extTrig(b.value) }) };

		view.onClose = { // don't forget this
			scope.quit;
			synth.free;
			trigBus.free;
			scopeBus.free;
			scopeBuf.free;
		};

		view.layout = VLayout.new([scope.view, s: 5],
			HLayout.new([button, s: 0, a: \left ], [trigButton, s: 0, a: \left], nil));
		view.layout.spacing_(2);
		// view.layout.margins_(1);
	} /* STD_scope.createPanel */



	createPanel2 {
		var button, scope, synth;
		view.background = Color.new(0.3,0.4,0.2); // // Gradients not yet in QT gui
		/* w.view.background = HiliteGradient(Color.rand(0.0,1.0),Color.rand(0.0,1.0),
		[\h,\v].choose, 100, rrand(0.1,0.9));    */

		scope = ScopeView.new(view);
		scope.bufnum = scopeBuf.bufnum;
		postln("1st scopeBuf.bufnum =" + scopeBuf.bufnum);

		// add a button to start and stop the displays.
		button = Button.new(view, 40 @ 20);
		button.states = [["Scope on", Color.black, Color.green],["Scope off", Color.white, Color.red]];
		button.action = {|view|
			if (view.value == 1) {
				synth = Synth.new(\STDscope2, [\buffer, scopeBuf], addAction: \addToTail);
			};
			if (view.value == 0) {
				synth.free;
			};
		};

		view.onClose = { // don't forget this
			synth.free;
			scopeBus.free;
			scopeBuf.free;
			scope.free;
		};
		view.layout = VLayout.new(
			[scope, stretch: 2 /*, align: \topLeft */ ],
			[button, s: 0, align: \topLeft],
			nil);
		view.layout.margins_(1);

	} /* createPanel2 */

	extTrig { arg t;
		synth.set(\trigSelect, t);
	}

} /* STD_scope */


STD_spectrum{
	var <view, fsv, button, button2, nyquist;

	// this is a normal constructor method
	*new {  // * indicates this is a class method
		^super.new.init()
	}

	init {
		this.createPanel;
	}

	createPanel {
		var d, dy, dBSpec, freqSpec, fScaler, gridX, gridY, sView, lView;

		view = View.new;
		view.resize_(5);
		view.minSize_(250@200);

		fsv = FreqScopeView(view, view.bounds);
		fsv.resize_(5);
		fsv.scope.waveColors_([Color.yellow]);
		fsv.specialSynthArgs_([\fftBufSize, 4096]);

		// why does Server.options.sampleRate return nil?
		nyquist = Server.default.actualSampleRate/2;
		// freqSpec = ControlSpec( 0, nyquist, \lin, 1, 200, "Hz");
		freqSpec = ControlSpec(0, nyquist, \lin, 1, 200, "Hz");
		dBSpec = ControlSpec(fsv.dbRange.neg, 0, \lin, 1, -10, "");

		// Unfortunately, GridLines (below) does not plot log axis ticks with \exp
		// Therefore, I have added some method overwrites in Extensions: "GridLinesExp.sc"
		// These will cause some compile warnings.

		// Level scale
		gridY = GridLines.new(dBSpec);
		lView = UserView(view, Rect(0, 0, 21, view.bounds.height));
		lView.fixedWidth = 21;
		lView.resize_(4);
		lView.background = Color.new(0.2, 0.2, 0.2);

		// Frequency scale
		gridX = GridLines.new(freqSpec);
		sView = UserView(view, Rect(0, 0, view.bounds.width, 15));
		sView.fixedHeight = 15;
		sView.resize_(8);
		sView.background = Color.new(0.2, 0.2, 0.2);

		// Run/freeze button
		button = Button.new(view, 40@20);
		button.states = [["Run", Color.black, Color.green], ["Freeze", Color.white, Color.red]];
		button.action = {|b| this.run(b.value == 1) };

		// Lin/log button
		button2 = Button.new(view, 40@20);
		button2.states = [["Lin", Color.black, Color.gray], ["Log", Color.white, Color.gray]];
		button2.action = { arg b;
			fsv.freqMode_(b.value);
			if (b.value == 1)
				{ freqSpec.warp = \exp; freqSpec.minval = 20 }
				{ freqSpec.warp = \lin; freqSpec.minval =  0 };
			gridX.spec = freqSpec;
			d.horzGrid_(gridX);
			sView.refresh;
		};

		// dB axis
		dy = DrawGrid.new(lView.bounds, nil, gridY);
		dy.fontColor_(Color.white);
		dy.gridColors_([Color.white, Color.white]);
		// dy.vertGrid_(gridY);
		// from source code of DrawGrid.test():
		lView.drawFunc = { arg v; dy.bounds = v.bounds.moveTo(0, 0) ; dy.draw; };

		// Frequency axis
		d = DrawGrid.new(sView.bounds, gridX, nil);
		d.fontColor_(Color.white);
		d.gridColors_([Color.white, Color.white]);
		// from source code of DrawGrid.test():
		sView.drawFunc = { arg v; d.bounds = v.bounds.moveTo(0,-4); d.draw; };

		// Frequency scaler (1...5x)
		fScaler = Slider(view, 60@20);
		fScaler.value = 0;
		fScaler.action_({|f|
			freqSpec.maxval = nyquist/(1+(4*f.value));
			gridX.spec = freqSpec;
			d.horzGrid_(gridX);
			fsv.scope.xZoom_(1+(4*f.value));
			sView.refresh;
		});

		view.layout = VLayout(
			HLayout([lView, s: 0], [fsv.asView, stretch: 2]),
			HLayout([21, s: 0], [sView, s: 2]),
			HLayout([21, s: 0], [button, s: 0, a: \left], [button2, s: 0, a: \left], [fScaler, s:0], [nil, s: 2])
		);
		view.layout.spacing_(2);
		view.onClose_({ fsv.kill; });
	}

	run { arg bRun;
		fsv.active_(bRun);
		button2.visible_(bRun == false);
	}

} /* STD_spectrum */


STD_audioIn{
	var <view, audioOuts, audioIns, <inputBus;

	// this is a normal constructor method
	*new { arg main; // * indicates this is a class method
		^super.new.init(main)
	}

	init { arg main;
		audioOuts = Server.local.options.numOutputBusChannels;
		audioIns  = Server.local.options.numInputBusChannels;
		inputBus = audioOuts;  /* index of first input, by default */
		// this.compileSynthDefs;
		this.createPanel;
	}

	*compileSynthDefs {
		SynthDef(\STDaudioIn, {
			arg inBus = 0, outBus = 0;
			var signal;

			signal = In.ar(inBus, 2);
			Out.ar(outBus, signal);
			};
		).send(Server.default);
	}  /* compileSynthDefs() */

	createPanel {
		var c, synth;

		synth = Synth.new(\STDaudioIn, [\inBus, inputBus], addAction: \addToHead);
		view = View.new();
		view.minSize = Size.new(70, 30);
		view.background = Color.hsv(1 / 8.0, 0.1, 1);
		view.onClose_({ synth.free; this.free });

		c = EZNumber(view, Rect(5, 5, 60, 20), "Input",
			ControlSpec(0, audioIns-1, 'lin', 1),
			{ |ez| inputBus = ez.value + audioOuts; synth.set(\inBus, inputBus) },
			inputBus-audioOuts, true, 40, 20);
		c.setColors(stringColor: Color.black, numBackground: Color.green);
	} /* createPanel */

} /* STD_audioIn */


STD_f1f2{
	var <view, fw, csf0, csf1, csb1, csf2, csb2, csGain;

	// this is a normal constructor method
	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		// this.compileSynthDefs;
		csf0 = ControlSpec( 10, 84, default: 45, units: "MIDI");
		csf1 = ControlSpec(200, 1200, default: 500, units: "Hz");
		csf2 = ControlSpec(400, 2500, default: 1500, units: "Hz");
		csb1 = ControlSpec(0.5, 50, default: 10, warp: \exp, units: "Q");
		csb2 = ControlSpec(0.5, 50, default: 10, warp: \exp, units: "Q");
		csGain = ControlSpec(-40, 10,default: 0, units: "dB");
		this.createPanel;
	}


	*compileSynthDefs {
		SynthDef(\STDf1f2, {
			arg note = 42, f1 = 650, f2 = 1050, rq1 = 0.1, rq2 = 0.1,
			gain = 0, outBus = 0;
			var pulse, signal;

			pulse  = Blip.ar(note.midicps, mul: gain.dbamp);
			signal = BLowPass.ar(pulse,  f1, rq1, mul: 0.5);
			signal = BLowPass.ar(signal, f2, rq2, mul: 0.5);
			Out.ar(outBus, signal);
			}, [0.3, 0.1, 0.1, 0.1, 0.1, 0.1]
		).send(Server.default);

/*		SynthDef(\STDf1f2, {
			arg note = 42, f1 = 650, f2 = 1050, rq1 = 0.1, rq2 = 0.1,
			gain = 0, outBus = 0;
			var pulse, signal;

			pulse = Blip.ar(note.midicps, mul: gain.dbamp);
			signal = TwoPole.ar( pulse, f1, 1 - rq1.squared, mul: 0.01);
			signal = TwoPole.ar(signal, f2, 1 - rq2.squared, mul: 0.1);
			// signal = RLPF.ar(signal, f1, rq1);
			// signal = RLPF.ar(signal, f2, rq2);
			Out.ar(outBus, signal);
			}, [0.3, 0.1, 0.1, 0.1, 0.1, 0.1]
		).send(Server.default); */
	}  /* compileSynthDefs() */

	createPanel {
		var b1, b2, synth, sl, sl2, slg, d, rc, u;

		synth = Synth.new(\STDf1f2);
		view = View.new();
		view.minSize = Size.new(230, 230);
		view.background = Color.gray(0.3);
		view.onClose_( { synth.free; this.free } );

		// Create a value grid underneath the Slider2D
		// 	How does one get more gridlines on the given axes???
		// 	ControlSpec.gridValues does not work.
		u = UserView(view, view.bounds.insetAll(10, 10, 30, 50)); // Rect(0, 0, 250, 250)
		rc = Rect(0, 0, u.bounds.width, u.bounds.height);
		d = DrawGrid(rc.insetBy(10, 10), csf1.grid, csf2.grid); // button radius = 10 ?
		d.fontColor_(Color.black);
		d.gridColors_([Color.white, Color.white]);  // x, y
		u.drawFunc = { d.draw };  // Add also an oblique line where F1=F2

		// Create the Slider2D
		sl2 = Slider2D(view, u.bounds)
		.action_({|sl2| synth.set(\f1, csf1.map(sl2.x), \f2, csf2.map(sl2.y))})
		.background_(Color.new(0.3, 0.3, 0.75, 0.05))
		.knobColor_(Color.blue)
		.setXYActive(csf2.unmap(1500),csf1.unmap(500));

		// Bandwidth controls b1, b2
		b1 = Knob(view, Rect(u.bounds.left+5, u.bounds.height+15, 25, 25))
		.action_({|knob| synth.set(\rq1, csb1.map(knob.value).reciprocal)})
		.valueAction_(csb1.unmap(10));

		b2 = Knob(view, Rect(u.bounds.left+40, u.bounds.height+15, 25, 25))
		.action_({|knob| synth.set(\rq2, csb2.map(knob.value).reciprocal)})
		.valueAction_(csb1.unmap(10));

		// Note (pitch) control sl
		sl = Slider(view, Rect(u.bounds.left+75, u.bounds.height+15, u.bounds.width-75, 25))
		.action_({|sl| synth.set(\note, csf0.map(sl.value))})
		.valueAction_(csf0.unmap(45));

		// Gain control slg
		slg = Slider(view, Rect(u.bounds.width+15, u.bounds.top, 15, u.bounds.height))
		.action_({|slg| synth.set(\gain, csGain.map(slg.value))})
		.valueAction_(csGain.unmap(0));

	} /* createPanel */

} /* STD_f1f2 */

STD_precedence{
	var <view, fw, csdelay, csgain1, csgain2, theMain;

	// this is a normal constructor method
	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		theMain = main;  // sole instance of STD_main
		// this.compileSynthDefs;
		csdelay = ControlSpec(-50, 50, step: 0.1, default: 0, units: "ms");
		csgain1 = ControlSpec(-30, 10, default: 0, units: "dB");
		csgain2 = ControlSpec(-30, 10, default: 0, units: "dB");
		this.createPanel;
	}

	*compileSynthDefs {
		SynthDef(\STDprecedence, {
			arg delay = 0.0, gain1 = 0, gain2 = 0, freq = 2.0, outBus = 0, trigBus = 0;
			var pulse, signal, fSignal, dSignal, f1 = 650, f2 = 1050, rq1 = 0.07, rq2 = 0.07, dsec;

			dsec = delay.clip(-0.05, 0.05) + 0.05;
			pulse = Impulse.ar(freq);
			signal = TwoPole.ar(pulse,  f1, 1 - rq1.squared, mul: 0.003);
			signal = TwoPole.ar(signal, f2, 1 - rq2.squared, mul: 0.1);
			fSignal = DelayC.ar(signal, 0.05, 0.05, mul: gain1.dbamp);
			dSignal = DelayC.ar(signal, 0.10, dsec, mul: gain2.dbamp);
			OffsetOut.ar(outBus, [fSignal, dSignal]);
			OffsetOut.ar(trigBus, pulse);
			}).send(Server.default);
	}  /* compileSynthDefs() */

	createPanel {
		var b1, b2, synth, sl, sl2, slg, d, rc, t, u;

		t = theMain.trigBus;
		synth = Synth.new(\STDprecedence, [\trigBus, t]);
		view = View.new();
		view.minSize = Size.new(230, 230);
		view.background = Color.blue(0.3);
		view.onClose_( { synth.free; this.free } );
		rc = view.bounds;

		// Delay control sl
		sl = EZSlider(view, Rect(rc.left+5, rc.top+5, rc.width-10, 25),
			label:"Right delay",
			controlSpec: csdelay,
			initAction: true,
			labelWidth: 60,
			numberWidth: 35,
			unitWidth: 16)
		.setColors(stringColor: Color.gray)
		.action_({|sl| synth.set(\delay, sl.value * 0.001)});

		// Gain controls b1, b2
		b1 = Knob(view, Rect(rc.left+5, rc.top+40, 25, 25))
		.action_({|knob| synth.set(\gain1, csgain1.map(knob.value))})
		.valueAction_(csgain1.unmap(0));

		b2 = Knob(view, Rect(rc.left+5+30, rc.top+40, 25, 25))
		.action_({|knob| synth.set(\gain2, csgain2.map(knob.value))})
		.valueAction_(csgain2.unmap(0));

	} /* createPanel */

} /* STD_precedence */

STD_combfilter {
	var <view, btnSrc, csDelay, csSideGain, myBuf;

	// this is a normal constructor method
	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		csDelay = ControlSpec(0.00002, 0.02, warp: \exp, default: 0, units: "s");
		csSideGain = ControlSpec(-30, 0, default: 0, units: "dB");
		myBuf = Buffer.cueSoundFile(Server.default, Platform.recordingsDir +/+ "sounds/your-sweet-love.wav", 0, 2);
		this.createPanel;
	}


	*compileSynthDefs {
		SynthDef(\STDcomb, {
			arg delay = 0.0, sideGain = 0.0, source=0.0, buf=0, outBus = 0;
			var signal, total, disk;

			disk = DiskIn.ar(2, buf, 1);
			signal = Select.ar(source, [PinkNoise.ar(0.3)!2, disk]);
			total = DelayN.ar(signal, 0.2, delay, sideGain.dbamp, signal);
			Out.ar(outBus, total);
			}
		).send(Server.default);
	}  /* compileSynthDefs() */

	createPanel {
		var synth, sl, sl2, slSrc, d, rc, u;

		synth = Synth.new(\STDcomb);
		synth.set(\buf, myBuf);
		view = View.new();
		view.minSize = Size.new(230, 230);
		view.background = Color.gray(0.3);
		view.onClose_( { synth.free; myBuf.free; this.free } );

		// Create a value grid underneath the Slider2D
		// 	How does one get more gridlines on the given axes???
		// 	ControlSpec.gridValues does not work.
		u = UserView(view, view.bounds.insetAll(10, 10, 30, 50)); // Rect(0, 0, 250, 250)
		rc = Rect(0, 0, u.bounds.width, u.bounds.height);
		d = DrawGrid(rc.insetBy(10, 10), csDelay.grid, csSideGain.grid); // button radius = 10 ?
		d.fontColor_(Color.white);
		d.gridColors_([Color.white, Color.white]);  // x, y
		u.drawFunc = { d.draw };

		// Create the Slider2D
		sl2 = Slider2D(view, u.bounds)
		.action_({|sl2| synth.set(\delay, csDelay.map(sl2.x), \sideGain, csSideGain.map(sl2.y))})
		.background_(Color.new(0.1, 0.1, 0.2, 0))
		.knobColor_(Color.blue)
		.setXYActive(csDelay.unmap(0.02),csSideGain.unmap(-20.0));

		btnSrc = Button(view, Rect(u.bounds.left+75, u.bounds.height+15, u.bounds.width-75, 25))
		.states_([
			["Pink noise", Color.black, Color.new(1, 0.6, 0.6)],
			["Sound file", Color.black, Color.cyan]
		])
		.action_({|b| synth.set(\source, b.value)})
		.valueAction_(0);

	} /* createPanel */

} /* STD_combfilter */


