/*
TODO:
1. Origanize the gui script with classes (x)
1.1 layout stretch
2. Synth chain: read the previous one in the bus, add filter, and out (x)
2.1 SynthDefs are predefined so that we can call one anytime to create a Synth. x
2.2 The chain consists of Synths and should be created in the right order. x
2.3 Use Array to save and control different Synths separately. x
3. MIDI control: Korg microKontrol, map knobs

Issues:
1. stretching is working weird
*/

Gui_main{
	var modules, src, filter;

	// this is a normal constructor method
	*new { // * indicates this is a class method
		/* | arga, argb, argc | */
		^super.new.init(/*arga, argb, argc*/)
	}

	init { /* | arga, argb, argc | */
		// ~timestamp = Date.getDate.format("%d%m%Y_%H%M");
		modules = [
			["Audio Source", "Vocal Tract Formants"],
			[Gui_source, Gui_filter]
		];
		Server.default.waitForBoot({
			postln("Server booted.");
			this.compileSynthDefs;
			Server.default.sync;
			this.createPanel;
			MIDIClient.init;
			MIDIClient.sources;
			MIDIIn.connectAll;
		});

	}

	compileSynthDefs{
		modules[1].do( {|class| if (class.notNil, { class.compileSynthDefs } )} )
	}

	createPanel{
		var window, srcView, filterView, filterView2D, notchView;
		var windowOrder, saveButton, loadButton;

		window = Window("Gui Demo", Rect(
			Window.screenBounds.width/2-100,
			Window.screenBounds.height/2-200,
			800,
			600), scroll: true)
		.front;
		// window.alwaysOnTop_(true);

		src = Gui_source.new();
		srcView = src.view;

		filter = Gui_filter.new();
		filterView = filter.view;
		filterView2D = filter.view2D;
		notchView = filter.viewNotch;

		windowOrder = Button(window, 20@20).font_(Font("Helvetica", 12))
		.states_([
			["Stick to Top", Color.black, Color.gray],
			["Stick to Top", Color.green, Color.gray]
		])
		.action_({arg butt;
			if(butt.value==1,
				{window.alwaysOnTop_(true)},
				{window.alwaysOnTop_(false)}
			);
		});
		loadButton = Button(window, 20@20).font_(Font("Helvetica", 12))
		.states_([
			["Load File", Color.black, Color.grey(0.6)]
		])
		.action_({
			FileDialog({ |path|
				postln("Selected file:" + path);
				postln("File type is:" + File.type(path));
				~loadPath = path;
			},
			fileMode: 1,
			stripResult: true,
			path: "D:/VowelSynthesizer/hifi-voice");
		});
		saveButton = Button(window, 20@20).font_(Font("Helvetica", 12))
		.states_([
			["Save to csv", Color.black, Color.grey(0.8)]
		])
		.action_({
			FileDialog({ |path|
				var file;
				~newFile = path++".csv";
				file = File(~newFile, "a");
				postln("Selected file:" + ~newFile);
				postln("File type is:" + File.type(~newFile));
				file.close;
			},
			fileMode: 0,
			stripResult: true,
			acceptMode: 1,
			path: "D:/VowelSynthesizer/hifi-voice");
		});

		window.layout = VLayout(
			[HLayout( [srcView, s:4, a:\left], [filterView2D, s:0, a:\left], [nil],
				[VLayout(
					[windowOrder, s:1, a:\topRight],
					[saveButton, s:1, a:\topRight],
					[loadButton, s:1, a:\topRight],
					[nil]
				)],
			)],
			[filterView],
			[notchView],
			[nil]
		);
		window.layout.margins_(1);

		window.onClose_({
			// todo: something
			MIDIIn.disconnectAll;
			MIDIClient.disposeClient;
			postf("===Synthesis Ends.===\n");
		});
	}

}

Gui_source{
	// todo: flutter
	// freq: 50-200 ms period time, <1% of F0
	// amp: varies from vowelsFT6T


	var <view, synth;
	// var midiOn, midiOff, noteBox;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		this.createPanel;
		// this.midiControl;
	}

	*compileSynthDefs {
		SynthDef(\Voxsource, {
			arg outBus=0,
			note=45, invQ=0.1, scale=1.4, mul=10.0,
			fv=6.0/*5..8*/, ampv=0.03,
			ff=4.0/*4..6*/, ampf=0.02, rqf=0.2;
			var source, vibrato, flutter;
			// flutter = RLPF.ar(WhiteNoise.ar(ampf), ff, rqf);
			flutter=1.0;
			vibrato = SinOsc.ar(fv, 0, ampv, 1);
			source = ApexSource01.ar(
				flutter*vibrato*note.midicps,
				invQ:invQ, scale:scale, mult:mul);
			// source = SoundIn.ar(0);
			Out.ar(outBus, source);
		};
		).send(Server.default);
	}

	createPanel {
		var title, button, noteBox, noteLabel, sLabel, sSlider, qLabel, qSlider;
		var volSlider, volLabel;
		var flutterLayout;
		var vFreq, vAmp; // vibrato
		var fFreq, fAmp, fQ; // flutter
		// var midiControl;
		var midiOn, midiOff;
		var saveButton, loadButton;

		synth = Synth.new(\Voxsource, [\outBus, 0]);
		view = View()
		.background_(Color.gray.alpha_(0.3));
		view.onClose_({ // Free synth when view is destroyed
			synth.free;
			midiOn.free;
			midiOff.free;
			this.free;
		});

		loadButton = Button(view, 20@20)
		.states_([["Load", Color.white, Color.grey(0.4)]]);
		loadButton.action_({
			var fr;
			fr = CSVFileReader.read(~loadPath, true, true);
			noteBox.valueAction_(fr[0][1].asInteger);
			volSlider.valueAction_(fr[0][3].asFloat);
			qSlider.valueAction_(fr[1][1].asFloat);
			sSlider.valueAction_(fr[2][1].asFloat);
			vFreq.valueAction_(fr[3][1].asFloat);
			vAmp.valueAction_(fr[4][1].asFloat);
		});

		saveButton = Button(view, 20@20)
		.states_([["Save", Color.white, Color.grey(0.2)]])
		.mouseDownAction_({
			var file;
			file = File(~newFile, "a");
			file.write("Pitch in MIDI note,"++noteBox.value.asString++
				",Volume,"++volSlider.value.asString ++ Char.nl);
			file.write("1/Q," ++ qSlider.value.asString++ Char.nl);
			file.write("Freq scale," ++ sSlider.value.asString++Char.nl);
			file.write("Vibrato freq,"++vFreq.value.asString++Char.nl);
			file.write("Vibrato amp,"++vAmp.value.asString++Char.nl++Char.nl);
			file.close;
		});

		// Source gui
		title = StaticText(view, 80@20).string_("Source sound").font_(Font("Helvetica", 12, bold:true));
		button = Button(view,30@20)
		.states_([
			["Mute", Color.black, Color.gray(0.8)],  // value=0
			["Unmute", Color.white, Color(0.4, 1.0, 0.6)]    // value=1
		]);
		button.action_({ |obj|
			if( obj.value == 1,
				{synth.set(\mul, 0)},{synth.set(\mul, 10)}
			);
		});

		noteLabel = StaticText(view, 60@20).string_("MIDI Note").font_(Font("Helvetica", 12));
		noteBox = NumberBox(view, 20@20).clipLo_(21).clipHi_(108).step_(1);
		noteBox.action_({ |obj|
			var f;
			f = obj.value;
			synth.set(\note, f);
		})
		.valueAction_(45);

		sLabel = StaticText(view, 40@20).string_("Freq scale").font_(Font("Helvetica", 12));
		sSlider = Slider(view, 40@20).value_(1.4.linlin(0.5, 3.0, 0, 1));
		sSlider.action_({ |obj|
			var scale;
			scale = obj.value.linlin(0, 1, 0.5, 3.0).postln;
			synth.set(\scale, scale);
		});

		qLabel = StaticText(view, 50@20).string_("Q(reciprocal)").font_(Font("Helvetica", 12));
		qSlider = Slider(view, 40@20).value_(2.0.linlin(0.5, 3.0, 0, 1));
		qSlider.action_({ |obj|
			var invQ;
			invQ = obj.value.linlin(0, 1, 0.5, 3.0).postln;
			synth.set(\invQ, invQ);
		});

		volLabel = StaticText(view, 20@20).string_("Vol").font_(Font("Helvetica", 12));
		volSlider = Slider(view, 20@60).value_(0.1);
		volSlider.action_({ |obj|
			var m;
			m = (obj.value.linexp(0, 1, 10, 30));
			synth.set(\mul, m);
			if( (obj.value > 0) && (button.value == 1), { button.value_(0); });
		});

		// Vibrato
		vFreq = NumberBox(view, 20@20).value_(6.0).clipLo_(4.0).clipHi_(8.0)
		.step_(0.1).scroll_step_(0.1)
		.action_({ |obj|
			var fv;
			fv = obj.value;
			synth.set(\fv, fv);
		});
		vAmp = NumberBox(view, 20@20).value_(0.03).decimals_(3).clipLo_(0.0).clipHi_(1.0)
		.step_(0.001).scroll_step_(0.001)
		.action_({ |obj|
			var ampv;
			ampv = obj.value;
			synth.set(\ampv, ampv);
		});

		// Flutter
		fFreq = NumberBox(view, 20@20).value_(4.0).clipLo_(4.0).clipHi_(6.0)
		.step_(0.1).scroll_step_(0.1)
		.action_({ |obj|
			var ff;
			ff = obj.value;
			synth.set(\ff, ff);
		});
		fAmp = NumberBox(view, 20@20).value_(0.02).decimals_(3).clipLo_(0.0).clipHi_(1.0)
		.step_(0.001).scroll_step_(0.001)
		.action_({ |obj|
			var ampf;
			ampf = obj.value;
			synth.set(\ampf, ampf);
		});
		fQ = NumberBox(view).value_(5).decimals_(2).step_(0.01).scroll_step_(0.01).clipHi_(6.0)
		.action_({ |obj|
			var rq;
			rq = 1/obj.value;
			synth.set(\rqf, rq);
		});


		// Layout
		flutterLayout = HLayout(
			[VLayout(
				[StaticText(view).string_("Vibrato").font_(Font("Helvetica", 12, bold:true)), a:\topLeft],
				[HLayout( [StaticText(view).string_("Freq[Hz]").font_(Font("Helvetica", 12))], [vFreq, a:\topRight], [nil] )],
				[HLayout( [StaticText(view).string_("Amp").font_(Font("Helvetica", 12))], [vAmp, a:\topRight], [nil] )]
			)],
			10,
			[VLayout(
				[StaticText(view).string_("Flutter").font_(Font("Helvetica", 12, bold:true))],
				[HLayout( [StaticText(view).string_("Freq[Hz]").font_(Font("Helvetica", 12))], [fFreq, a:\right], [nil] )],
				[HLayout( [StaticText(view).string_("Amp").font_(Font("Helvetica", 12))], [fAmp, a:\right], [nil] )],
				[HLayout( [StaticText(view).string_("Filter Q").font_(Font("Helvetica", 12))], [fQ, a:\right], [nil])]
			)],
			[nil]
		);

		view.layout = HLayout(
			[VLayout(
				[HLayout([saveButton], [loadButton], [nil])],
				[HLayout([title], [button], [nil])],
				[HLayout([noteLabel], [noteBox, a:\left], [nil])],
				[HLayout([qLabel, a:\left], [qSlider, s:5], [nil])],
				[HLayout([sLabel, a:\left], [sSlider, s:5], [nil])],
				10,
				[flutterLayout],
				[nil]
			)],
			20,
			[VLayout([volLabel], [volSlider], [nil])],
			[nil]
		);

		// MIDI control
		midiOn = MIDIFunc.noteOn({ |veloc, num, chan, src|
			postf("MIDI note % on\n", num-12);
			synth.set(\note, num-12);
			{noteBox.value_(num-12)}.defer;
		});
		/*midiOff = MIDIFunc.noteOff({ |veloc, num, chan, src|
		notes[num].release;
		});*/
	}
}

Gui_filter{
	var <view, <view2D, <viewNotch, formantViews, formantReView, notchViews, notchReView;
	var reLayoutFormant, reLayout2D;
	var synths, numFormants, freqs, invQs/*reciprocal of Q*/;
	var notchSynths, numNotches, notchFreqs, notchQs;
	var lowLevel, notchLevel, lowKnob, notchKnob;
	var freqBoxes, qBoxes;
	var csf1, csf2, csInvQ, csff;
	var knobCC, sliderCC;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		numFormants = 0;
		numNotches = 0;
		csff = Array.fill(10, ControlSpec(400, 8000, default: 1700, units: "Hz"));
		csf1 = ControlSpec(200, 1200, default: 500, units: "Hz");
		csf2 = ControlSpec(400, 2500, default: 1050, units: "Hz");
		csff[0] = csf1; csff[1] = csf2;
		csInvQ = ControlSpec(0, 127, warp:'lin', step:1, default:6.0);
		// freqBoxes = [];
		// qBoxes = [];
		synths = [];
		freqs = [500, 1000, 2400, 2700, 3000];
		invQs = Array.fill(5, 0.05);
		notchSynths = [];
		notchFreqs = [800, 1500, 3500];
		notchQs = Array.fill(5, 0.1);
		lowLevel = 0.1; notchLevel = 0.1;
		numFormants = 0; numNotches = 0;
		this.createPanel;
		this.createPanelNotch;
		this.midiControl;
	}

	*compileSynthDefs {
		SynthDef(\Voxformant, {
			arg inBus=0, outBus=0,
			freq = 500, // default freq of F1
			res=0.1, low=0.1, band=0.0, high=0.0, notch=0.0, peak=0.0; //levels
			var signal;

			signal = In.ar(inBus, 1);  // get the signal from inBus
			signal = SVF.ar(signal,    // add a filter
				freq, // cutoff freq
				res,  // mysterious factor :-\
				low, band, high, notch, peak, mul: 5);
			ReplaceOut.ar(outBus, signal);
		};
		).send(Server.default);
	}

	loadSettings {
		var fr;
		synths = [];

		fr = CSVFileReader.read(~loadPath, true, true);
		lowLevel = fr[5][1].asFloat;
		{lowKnob.valueAction_(lowLevel)}.defer;
		freqs = fr[7].asFloat; numFormants = freqs.size;
		freqs.postln;
		invQs = 1/(fr[11].asFloat);
		numFormants.do({ |i|
			var synth;
			synth = Synth(\Voxformant,
				[\inBus, 0, \outBus, 0, \freq, freqs[i], \res, invQs[pow(1-invQs[i], 4)], \low, lowLevel],
					addAction: \addToTail);
			synths = synths.add(synth);
		});

		if ( fr[18] != nil, {
			notchSynths = [];
			notchLevel = fr[12][1].asFloat;
			{notchKnob.valueAction_(notchLevel)}.defer;
			notchFreqs = fr[14].asFloat; numNotches = notchFreqs.size;
			notchFreqs.postln;
			notchQs = 1/(fr[18].asFloat);
			numNotches.do({ |i|
				var synth;
				synth = Synth(\Voxformant,
					[\inBus, 0, \outBus, 0, \freq, notchFreqs[i], \res, invQs[pow(1-notchQs[i], 4)],
					 \low, 0, \notch, notchLevel],
					addAction: \addToTail);
				notchSynths = notchSynths.add(synth);
			});
		} );
		this.reLayoutNotch;
		this.reLayoutFormant;
		this.reLayout2D;
	}

	saveFilters { arg filterName, resName, level, num, ff, iqq;
		var file;
		file = File(~newFile, "a");
		file.write(filterName + "level," ++ level.asString ++ Char.nl);
		file.write(resName++Char.nl);
		num.do({|i|
			file.write(ff[i].asString ++ if ( i!=(num-1), ",", Char.nl ));
		});
		file.write("Res"++Char.nl);
		num.do({|i|
			file.write(pow(1-iqq[i], 4).asString ++ if ( i!=(num-1), ",", Char.nl ));
		});
		file.write("Q"++Char.nl);
		num.do({|i|
			file.write((1/iqq[i]).asString ++ if ( i!=(num-1), ",", Char.nl ));
		});
		file.write(Char.nl);
		file.close;
	}

	saveSettings {
		// formants
		this.saveFilters("Low", "Formants", lowLevel, numFormants, freqs, invQs);
		// notches
		if( numNotches > 0,
			this.saveFilters("Notch", "Notches", notchLevel, numNotches, notchFreqs, notchQs) );
	}

	midiControl {
		knobCC = MIDIFunc.cc(
			{ |cc, ccNum, chan, src| {this.knobControl(cc, ccNum)}.defer },
			(21..28)
		);
		sliderCC = MIDIFunc.cc(
			{ |cc, ccNum, chan, src| {this.sliderControl(cc, ccNum)}.defer },
			(11..18)
		);
	}

	knobControl { arg ccVal, ccNum;
		var fQ, res; // reciprocal
		fQ = csInvQ.unmap(ccVal);
		res = pow(1-fQ, 4);
		postf("res=%\n", res);
		synths[ccNum-21].set(\res, res);
		invQs[ccNum-21] = fQ;
		// qBoxes[ccNum-21].value_(1/fQ);
		this.reLayout2D;
		this.reLayoutFormant;
	}

	sliderControl { arg ccVal, ccNum;
		var ff, cs;
		if ( csff[ccNum-11]!=Nil,
			{cs=csff[ccNum-11]},
			{cs=ControlSpec(400, 8000, default: 1700, units: "Hz")} );
		ff = cs.map(ccVal/127.0);
		postf("midi slider % - % - freq %\n", ccNum, ccVal, ff);
		synths[ccNum-11].set(\freq, ff);
		freqs[ccNum-11] = ff;
		// freqBoxes[ccNum-11].value_(ff);
		this.reLayout2D;
		this.reLayoutFormant;
	}

	reLayout2D {
		if( numFormants >= 2,
			{ view2D.removeAll;
			  this.createPanel2D;
			  view2D.refresh; },
			{ view2D.removeAll }
		);
	}

	reLayoutFormant {
		// renew the synth once a new formant/notch is enabled/disabled
		formantViews = [];
		numFormants.do( {|i| this.createSubPanel(i)} );
		formantReView.removeAll;
		formantReView.layout = HLayout(*formantViews); // an array of sub-views
		numFormants.do({|i|
			formantReView.layout.setStretch(i, 4);
			formantReView.layout.setAlignment(i, \left)});
		formantReView.refresh;
	}

	reLayoutNotch {
		// renew the synth once a new notch is enabled
		notchViews = [];
		numNotches.do({|i| this.createSubPanelNotch(i);} );
		notchReView.removeAll;
		notchReView.layout = HLayout(*notchViews); // an array of sub-views
		numNotches.do({ |i|
			notchReView.layout.setStretch(i, 4);
			notchReView.layout.setAlignment(i, \left) });
		notchReView.refresh;
	}

	reLayout { arg views, num, reView;
		// renew the synth once a new notch is enabled
		views = [];
		num.do({|i| this.createSubPanelNotch(i);} );
		reView.removeAll;
		reView.layout = HLayout(*views); // an array of sub-views
		reView.refresh;
	}

	// FORMANT 1, 2
	createPanel2D {
		var formantSlider2D;
		formantSlider2D = Slider2D(view2D)
		// .minSize_(200@200)
		.x_(csf1.unmap(freqs[0])).y_(csf2.unmap(freqs[1])); // x: F1, y: F2
		formantSlider2D.action_( {
			|sl|
			synths[0].set(\freq, csf1.map(sl.x));
			synths[1].set(\freq, csf2.map(sl.y));
			freqs[0] = csf1.map(sl.x);
			freqs[1] = csf2.map(sl.y);
			this.reLayoutFormant.();
		} );
		view2D.layout = VLayout(
			[StaticText(view2D).string_("Vowel").font_(Font("Helvetica", 12, bold:true)), a:\topLeft],
			[StaticText(view2D).string_("F1").font_(Font("Helvetica", 12)), a:\leftTop],
			[HLayout(
				[formantSlider2D, s:0, a:\left],
				[StaticText(view2D).string_("F2").font_(Font("Helvetica", 12)), a:\rightBottom], [nil]), s:0],
			[nil]
		);
	}
	// FORMANT CONTROL
	createPanel {
		var newButton, removeButton, lowLabel/*, lowKnob*/;
		var saveButton, loadButton;

		// view
		view = View().minSize_(Size(400, 100));
		formantReView = View();
		// free synths when view is destroyed
		view.onClose_({
			synths.do({|si, i| si.free;});
			this.free;
		});
		view2D = View();

		// Buttons
		loadButton = Button(view, 20@20)
		.states_([["Load", Color.white, Color.grey(0.4)]]);
		loadButton.action_({
			this.loadSettings();
		});

		saveButton = Button(view, 20@20)
		.states_([["Save", Color.white, Color.grey(0.2)]])
		.mouseDownAction_({
			this.saveSettings();
		});

		newButton = Button(view, 20@20) // add a new formant
		.states_( [ ["+", Color.red, Color.gray(0.8)] ] );
		newButton.mouseDownAction_({
			var synth, freq, res; // create synth
			if( freqs[numFormants] == nil,
				{freqs = freqs.add(5000); invQs = invQs.add(0.05);} );
			freq = freqs[numFormants];
			res = pow(1-invQs[numFormants], 4);
			// create a new formant and add the synth into the chain
			synth = Synth(\Voxformant,
				[\inBus, 0, \outBus, 0, \freq, freq, \res, res],
				addAction: \addToTail);
			synths = synths.add(synth);
			numFormants = numFormants + 1;
			this.reLayout2D;
			this.reLayoutFormant;
		});
		removeButton = Button(view, 20@20) // remove a formant
		.states_( [ ["-", Color.black, Color.gray(0.8)] ] );
		removeButton.mouseDownAction_({
			if(	numFormants > 0, {
				numFormants = numFormants - 1;
				synths[numFormants].free; // free synth node
				synths.removeAt(numFormants); // remove synths[numFormants]
				freqs.removeAt(numFormants);
				this.reLayout2D;
				this.reLayoutFormant;
			} )
		});
		// Levels
		lowLabel = StaticText(view, 20@20).string_("Low").font_(Font("Helvetica", 12, bold:true));
		lowKnob = Knob(view, 20@20).value_(0.1);
		lowKnob.action_({
			arg obj;
			var lv;
			lv = obj.value;
			postf("low %.\n", lv);
			synths.do( {|si, i| si.set(\low, lv)} )
		});
		// Layout
		view.layout = VLayout(
			[ HLayout (
				[saveButton],
				[loadButton],
				[nil] ), s:1, a:\left],
			[ HLayout( [formantReView, a:\left],
				[VLayout(
					[newButton],
					[removeButton],
					[VLayout( [lowLabel, a:\center], [lowKnob], [nil] )],
					[nil]
				)],
			[nil] ) ],
			[nil]
		);
	}

	createSubPanel { arg i;/*, rangeLo, rangeHi;*/
		var subView, synth;
		var fLabel, fUnit, freqBox, qLabel, qBox, resValue;

		subView = View()
		.minSize_(Size(40, 80))
		.background_(Color(0.5, 0.5, 1)); // blueish

		// Gui components
		fLabel = StaticText(subView).string_("Formant " + (i+1)).font_(Font("Helvetica", 12, bold:true)); // the i-th formant
		fUnit = StaticText(subView, 20@20).string_("Hz").font_(Font("Helvetica", 12));
		freqBox = NumberBox(subView, 40@20).step_(0.1).decimals_(1)/*.value_(freqs[i])*/;
		qLabel = StaticText(subView, 40@20).string_("Q").font_(Font("Helvetica", 12));
		qBox = NumberBox(subView, 40@20).clipLo_(1).step_(0.1);
		// freqBoxes.add(freqBox);
		// qBoxes.add(qBox);
		resValue = StaticText(subView, 20@20).font_(Font("Helvetica", 12))
		.string_("Res"+pow(1 - (1/qBox.value), 4).round(0.1**3));


		// Gui actions
		freqBox.action_({
			arg obj;
			var f;
			f = obj.value;
			freqs[i] = f;
			synths[i].set(\freq, f);
			if( i <= 2,
				{this.reLayout2D.()} );
		});
		qBox.action_({
			arg obj;
			var res;
			invQs[i] = 1/obj.value;
			res = pow(1 - (1/obj.value), 4);
			resValue.string = "Res"+res.round(0.1**3);
			synths[i].set(\res, res);
		});
		freqBox.valueAction_(freqs[i]);
		qBox.valueAction_(1/invQs[i]);

		// Layout
		subView.layout = VLayout(
			[fLabel],
			[HLayout( [freqBox, s:1, a:\left], [fUnit, s:1, a:\left], [nil] )],
			[qLabel],
			[qBox, s:1, a:\left],
			[resValue],
			[nil]
		);

		formantViews = formantViews.add(subView);
	}

	// NOTCH CONTROL
	createPanelNotch {
		var newButton, removeButton, notchLabel/*, notchKnob*/;
		// var saveButton;

		// view
		viewNotch = View().minSize_(Size(400, 100));
		notchReView = View();
		// free synths when view is destroyed
		viewNotch.onClose_({
			notchSynths.do({|si, i| si.free;});
			this.free;
		});

		// Buttons
		newButton = Button(viewNotch, 20@20) // add a new notch
		.states_( [ ["+", Color.red, Color.gray(0.8)] ] );
		newButton.mouseDownAction_({
			var synth, freq, res; // create synth
			if( notchFreqs[numNotches] == nil,
				{notchFreqs = notchFreqs.add(6000); notchQs = notchQs.add(0.1);}
			);
			freq = notchFreqs[numNotches];
			res = pow(1-notchQs[numNotches], 4);
			// create a new notch and add the synth into the chain
			synth = Synth(\Voxformant,
				[\inBus, 0, \outBus, 0, \freq, freq, \res, res, \low, 0, \notch, 0.1],
				addAction: \addToTail);
			notchSynths = notchSynths.add(synth);
			numNotches = numNotches + 1;
			this.reLayoutNotch;
		});
		removeButton = Button(viewNotch, 20@20) // remove a notch
		.states_( [ ["-", Color.black, Color.gray(0.8)] ] );
		removeButton.mouseDownAction_({
			if(	numNotches > 0, {
				numNotches = numNotches - 1;
				notchSynths[numNotches].free; // free synth node
				notchSynths.removeAt(numNotches); // remove synths[numNotches]
				notchFreqs.removeAt(numNotches);
				this.reLayoutNotch;
			}
			)
		});
		// Levels
		notchLabel = StaticText(viewNotch, 20@20).string_("Notch").font_(Font("Helvetica", 12, bold:true));
		notchKnob = Knob(viewNotch, 20@20).value_(0.1);
		notchKnob.action_({
			arg obj;
			var lv;
			lv = obj.value;
			postf("notch %.\n", lv);
			notchSynths.do( {|si, i| si.set(\notch, lv)} )
		});

		// Layout
		viewNotch.layout = VLayout(
			[HLayout(
				[notchReView, a:\left],
				[VLayout(
					[newButton],
					[removeButton],
					[VLayout(
						[notchLabel, a:\center], [notchKnob], [nil]
					)],
					[nil] )],
				[nil]
			)],
			[nil]
		);
	}

	createSubPanelNotch{ arg i;
		var synth;
		var subView, fLabel, fUnit, freqBox, qLabel, qBox, resValue;

		subView = View()
		.minSize_(Size(40, 60))
		.background_(Color(0.5, 0.5, 1)); // blueish

		// Gui components
		fLabel = StaticText(subView, 40@20).string_("Notch " + (i+1)).font_(Font("Helvetica", 12, bold:true));
		fUnit = StaticText(subView, 20@20).string_("Hz").font_(Font("Helvetica", 12));
		freqBox = NumberBox(subView, 40@20)
		.step_(0.1).decimals_(1)/*.value_(notchFreqs[i])*/;
		qLabel = StaticText(subView, 40@20).string_("Q").font_(Font("Helvetica", 12));
		qBox = NumberBox(subView, 40@20)/*.value_(1/notchQs[i])*/.clipLo_(1).step_(0.1);
		resValue = StaticText(subView, 20@20).font_(Font("Helvetica", 12))
		.string_("Res"+pow(1 - (1/qBox.value), 4).round(0.1**3));

		// Gui actions
		freqBox.action_({ |obj|
			var f;
			f = obj.value;
			notchFreqs[i] = f;
			notchSynths[i].set(\freq, f);
		});
		qBox.action_({ |obj|
			var res;
			notchQs[i] = 1/obj.value;
			res = pow(1 - (1/obj.value), 4);
			resValue.string = ("Res "+res.round(0.1**3));
			notchSynths[i].set(\res, res);
		});
		freqBox.valueAction_(notchFreqs[i]);
		qBox.valueAction_(1/notchQs[i]);

		// Layout
		subView.layout = VLayout(
			[fLabel],
			[HLayout( [freqBox, s:1, a:\left], [fUnit, s:1, a:\left], [nil] )],
			[qLabel],
			[qBox, s:1, a:\left],
			[resValue],
			[nil]
		);

		notchViews = notchViews.add(subView);
	}
}